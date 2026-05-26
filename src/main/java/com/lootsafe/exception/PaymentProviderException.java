package com.lootsafe.exception;

public class PaymentProviderException extends RuntimeException {

    private final Integer providerStatusCode;
    private final String providerDetail;

    public PaymentProviderException(String message, Integer providerStatusCode, String providerDetail, Throwable cause) {
        super(message, cause);
        this.providerStatusCode = providerStatusCode;
        this.providerDetail = providerDetail;
    }

    public Integer getProviderStatusCode() {
        return providerStatusCode;
    }

    public String getProviderDetail() {
        return providerDetail;
    }
}
