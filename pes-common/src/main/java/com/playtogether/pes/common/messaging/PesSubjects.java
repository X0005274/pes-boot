package com.playtogether.pes.common.messaging;

import com.playtogether.pes.common.model.EntityType;

/**
 * PES.&lt;layer&gt;.&lt;domain&gt;.&lt;type&gt; 패턴의 TIBCO RV Subject 를 enum 조합으로 생성한다.
 * Reply Subject 는 RV _INBOX 로 자동 생성되므로 여기서 정의하지 않는다.
 *
 * <pre>
 *   PesSubjects.of(Layer.UI,  EntityType.LOT, Type.REQUEST) -&gt; "PES.UI.LOT.REQUEST"
 *   PesSubjects.of(Layer.BIZ, EntityType.LOT, Type.EVENT)   -&gt; "PES.BIZ.LOT.EVENT"
 * </pre>
 */
public final class PesSubjects {

    public enum Layer { UI, BIZ }

    public enum Type { REQUEST, EVENT }

    private static final String SYSTEM = "PES";

    private PesSubjects() {
    }

    public static String of(Layer layer, EntityType domain, Type type) {
        return String.join(".", SYSTEM, layer.name(), domain.name(), type.name());
    }
}
