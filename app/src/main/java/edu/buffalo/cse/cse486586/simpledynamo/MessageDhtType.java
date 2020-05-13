package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.Serializable;

public enum MessageDhtType implements Serializable {
    INSERT(0),
    QUERYGLOBAL(1),
    QUERYSINGLE(2),
    DELETE(3);

    int scope;
    MessageDhtType(int scope) {
        this.scope = scope;
    }


}
