package org.example.session;

import lombok.Getter;
import lombok.Setter;
import org.example.player.Player;

import java.util.Collection;

@Getter
@Setter
public class Session {
    private Integer turnCounter;
    private Boolean dayNightCycle;
    private Collection<Player> players;
}
