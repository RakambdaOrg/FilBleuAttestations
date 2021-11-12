package fr.raksrinana.filbleuattestations.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
public class Card{
	@JsonProperty("id")
	private long id;
	@JsonProperty("uid")
	private String uid;
	@JsonProperty("recipientEmail")
	private String recipientEmail;
	@JsonProperty("downloaded")
	private Set<String> downloaded = new HashSet<>();
	
	@Override
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(!(o instanceof Card card)){
			return false;
		}
		return getId() == card.getId();
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(getId());
	}
}
