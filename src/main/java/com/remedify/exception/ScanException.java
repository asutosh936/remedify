package com.remedify.exception;

public class ScanException extends RuntimeException {

  private final String errorCode;

  public ScanException(String message) {
    super(message);
    this.errorCode = "SCAN_ERROR";
  }

  public ScanException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "SCAN_ERROR";
  }

  public ScanException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ScanException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
