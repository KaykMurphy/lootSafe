package com.lootsafe;

import com.lootsafe.enums.MessageAuthor;
import com.lootsafe.enums.MessageType;
import com.lootsafe.enums.PixKeyType;
import com.lootsafe.enums.ProductCategory;
import com.lootsafe.enums.Roles;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.model.ChatMessage;
import com.lootsafe.model.Offer;
import com.lootsafe.model.User;
import com.lootsafe.repository.ChatMessageRepository;
import com.lootsafe.repository.OfferRepository;
import com.lootsafe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "lootsafe.security.encryption-key=1234567890123456",
        "lootsafe.security.admin-api-key=test-admin-key",
        "lootsafe.secret-key=test-webhook-secret",
        "lootsafe.access-token=TEST-access-token"
})
class SecurityAndOfferResponseTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        chatMessageRepository.deleteAll();
        offerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void mediationEndpointRequiresApiKey() throws Exception {
        mockMvc.perform(get("/api/mediation/offers/statistics/profit"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Access denied: invalid API key"));
    }

    @Test
    void mediationEndpointAcceptsTrimmedApiKey() throws Exception {
        mockMvc.perform(get("/api/mediation/offers/statistics/profit")
                        .header("X-API-KEY", " test-admin-key "))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void offerDropRouteRequiresAuthentication() throws Exception {
        Offer offer = createOffer(TransactionStatus.IN_MEDIATION);

        mockMvc.perform(post("/api/offers/{id}/mediation/drop", offer.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void buyerCanDropMediationWhenAuthenticated() throws Exception {
        Offer offer = createOffer(TransactionStatus.IN_MEDIATION);
        createUser("buyer", "buyer@example.com", Roles.BUYER);

        mockMvc.perform(post("/api/offers/{id}/mediation/drop", offer.getId())
                        .with(user("buyer").roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("SETTLED"));
    }

    @Test
    void nonBuyerCannotDropMediation() throws Exception {
        Offer offer = createOffer(TransactionStatus.IN_MEDIATION);
        createUser("other", "other@example.com", Roles.BUYER);

        mockMvc.perform(post("/api/offers/{id}/mediation/drop", offer.getId())
                        .with(user("other").roles("BUYER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void adminDropRouteStillRequiresApiKey() throws Exception {
        Offer offer = createOffer(TransactionStatus.IN_MEDIATION);

        mockMvc.perform(post("/api/mediation/offers/{id}/drop", offer.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminDropRouteAcceptsApiKey() throws Exception {
        Offer offer = createOffer(TransactionStatus.IN_MEDIATION);

        mockMvc.perform(post("/api/mediation/offers/{id}/drop", offer.getId())
                        .header("X-API-KEY", "test-admin-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("SETTLED"));
    }

    @Test
    void buyerCanReadOfferMessageHistory() throws Exception {
        Offer offer = createOffer(TransactionStatus.IN_MEDIATION);
        createUser("buyer", "buyer@example.com", Roles.BUYER);
        createMessage(offer, "Mensagem em mediação");

        mockMvc.perform(get("/api/offers/{id}/messages", offer.getId())
                        .with(user("buyer").roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].messageText").value("Mensagem em mediação"));
    }

    @Test
    void nonParticipantCannotReadOfferMessageHistory() throws Exception {
        Offer offer = createOffer(TransactionStatus.IN_MEDIATION);
        createUser("other", "other@example.com", Roles.BUYER);
        createMessage(offer, "Mensagem em mediação");

        mockMvc.perform(get("/api/offers/{id}/messages", offer.getId())
                        .with(user("other").roles("BUYER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void publicOfferListDoesNotExposeSensitiveFields() throws Exception {
        String payload = """
                {
                  "productCategory": "GAME_ACCOUNT",
                  "description": "Ranked account",
                  "grossAmount": 100.00,
                  "trialPeriodHours": 24,
                  "credentialLogin": "seller-login",
                  "credentialPassword": "seller-password",
                  "sellerEmail": "seller@example.com",
                  "pixKeyType": "EMAIL",
                  "pixKey": "seller@example.com"
                }
                """;

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description").value("Ranked account"))
                .andExpect(jsonPath("$.content[0].credentialLogin").doesNotExist())
                .andExpect(jsonPath("$.content[0].credentialPassword").doesNotExist())
                .andExpect(jsonPath("$.content[0].sellerEmail").doesNotExist())
                .andExpect(jsonPath("$.content[0].pixQrCode").doesNotExist())
                .andExpect(jsonPath("$.content[0].pixCopyPaste").doesNotExist())
                .andExpect(jsonPath("$.content[0].mercadoPagoPaymentId").doesNotExist());
    }

    private Offer createOffer(TransactionStatus status) {
        Offer offer = new Offer();
        offer.setProductCategory(ProductCategory.GAME_ACCOUNT);
        offer.setDescription("Ranked account");
        offer.setGrossAmount(BigDecimal.valueOf(100));
        offer.setPlatformFee(BigDecimal.TEN);
        offer.setNetAmount(BigDecimal.valueOf(90));
        offer.setTrialPeriodHours(24);
        offer.setTransactionStatus(status);
        offer.setCredentialLogin("seller-login");
        offer.setCredentialPassword("seller-password");
        offer.setSellerEmail("seller@example.com");
        offer.setBuyerEmail("buyer@example.com");
        offer.setPixKeyType(PixKeyType.EMAIL);
        offer.setPixKey("seller@example.com");
        offer.setMercadoPagoPaymentId(123456789L);
        return offerRepository.save(offer);
    }

    private User createUser(String name, String email, Roles... roles) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("password");

        for (Roles role : roles) {
            user.getRoles().add(role);
        }

        return userRepository.save(user);
    }

    private ChatMessage createMessage(Offer offer, String content) {
        ChatMessage message = new ChatMessage();
        message.setOffer(offer);
        message.setAuthor(MessageAuthor.BUYER);
        message.setContent(content);
        message.setMessageType(MessageType.CHAT);
        return chatMessageRepository.save(message);
    }
}
