package ca.bc.gov.educ.api.student.profile.email.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Builder
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UMPRequestEmailVerificationEntity extends BaseEmailEntity {
  @NotNull(message = "studentRequestId can not be null.")
  private String studentRequestId;
  @NotNull(message = "Identity Type Label can not be null.")
  private String identityTypeLabel;
  @NotNull(message = "verificationUrl can not be null.")
  private String verificationUrl; // this holds the url link.
  @NotNull(message = "jwt Token can not be null.")
  private String jwtToken;
}
