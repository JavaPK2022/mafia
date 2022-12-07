package org.example.session;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Turn implements Serializable {
    private Integer userId;
    private String choice;
}
