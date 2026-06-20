package com.playtogether.pes.rv.tibco;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RV 접속 파라미터. application.yml 의 pes.rv.* (모두 OS 환경변수 주입) 와 매핑.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "pes.rv")
public class TibrvProperties {

    /** RVD service (예: 7500). */
    private String service = "7500";

    /** RVD network (예: ";" 또는 ";multicast"). */
    private String network = ";";

    /** RVD daemon (예: tcp:7500). */
    private String daemon = "tcp:7500";
}
