package org.apache.ignite.internal.commandline.cache.argument;

import org.apache.ignite.internal.commandline.argument.CommandArg;

public enum IndexesListArg implements CommandArg {
    ;
    /** Argument name. */
    private final String name;

    /**
     * @param name Argument name.
     */
    IndexesListArg(String name) {
        this.name = name;
    }

    /**
     * @return Option name.
     */
    @Override public String argName() {
        return name;
    }
}
