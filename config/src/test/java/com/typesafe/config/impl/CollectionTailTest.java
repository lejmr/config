package com.typesafe.config.impl;

import com.typesafe.config.*;
import org.junit.Test;

public class CollectionTailTest {
    @Test
    public void rejectsIncompatibleCollectionTails() {
        for (String source : new String[] {
                "x=[1]suffix", "x={a:1}suffix", "x=[1] suffix", "x=[1]\"suffix\"",
                "x={a:1} suffix", "x=[1]suffix[2]",
                // Exact reproductions from upstream issue #685.
                "list = [0, 1] | [2,3]",
                "list = [0] bar baz [1,2,3]",
                "list = [0] abc [bar, baz] ||| xyz [1,2,3]" }) {
            try {
                ConfigFactory.parseString(source).resolve();
                throw new AssertionError("discarded incompatible string: " + source);
            } catch (ConfigException.WrongType expected) { }
        }
        Config c = ConfigFactory.parseString("x=[1] \t [2]\ny={a:1} {b:2}\nz=[1] {0:2}\nw=[1] ${?NATIVE_TEST_ABSENT}")
            .resolve(ConfigResolveOptions.defaults().setUseSystemEnvironment(false));
        if (!c.getIntList("x").toString().equals("[1, 2]") || c.getInt("y.a") != 1 || c.getInt("y.b") != 2
                || !c.getIntList("z").toString().equals("[1, 2]") || c.getIntList("w").size() != 1)
            throw new AssertionError("collection concatenation controls");

    }
}
