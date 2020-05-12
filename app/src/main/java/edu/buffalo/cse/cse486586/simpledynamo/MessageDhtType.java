package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.Serializable;

public enum MessageDhtType implements Serializable {
    JOINREQUEST,
    JOINRESPONSE,
    JOINUPDATE,
    INSERT,
    QUERYGLOBAL,
    QUERYSINGLE,
    QUERYSINGLERESPONSE,
    DELETE
}
