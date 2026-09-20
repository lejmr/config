package com.typesafe.config.impl;

import com.typesafe.config.*;
import org.junit.Test;

public final class ReferencedObjectBarrierTest {
    private static void check(String source, String expected) {
        Config actual = ConfigFactory.parseString(source).resolve(ConfigResolveOptions.noSystem());
        Config wanted = ConfigFactory.parseString(expected).resolve(ConfigResolveOptions.noSystem());
        if (!actual.root().unwrapped().equals(wanted.root().unwrapped()))
            throw new AssertionError(actual.root().render() + " != " + wanted.root().render());
    }
    @Test
    public void keepsSourceBarriersOutOfReceivingObject() {
        check("v={x=1}\nr=null\nr=${v}\na={helper=0}\na=${r}",
                "v={x=1}\nr={x=1}\na={helper=0,x=1}");
        check("a={v={x=1},r=null,r=${a.v}}\na=${?MISSING}${a.r}",
                "a={v={x=1},r={x=1},x=1}");
        check("a={v={x=1},r=null,r=${a.v}}\na=${a.r}",
                "a={v={x=1},r={x=1},x=1}");
        check("v={x=1}\nr={old=2}\nr=null\nr=${v}\na={helper=0}\na=${r}",
                "v={x=1}\nr={x=1}\na={helper=0,x=1}");
        check("v={x=1}\nr=null\nr=${v}\na={helper=0}\na=null\na=${r}",
                "v={x=1}\nr={x=1}\na={x=1}");
        check("v={nested={old=2},nested=null,nested={x=1}}\na={nested={helper=0}}\na=${v}",
                "v={nested={x=1}}\na={nested={helper=0,x=1}}");
        check("a={r={x=1},v={x=1}}\na=${?MISSING}${a.r}",
                "a={r={x=1},v={x=1},x=1}");
        for (String observer : new String[] {"first", "other", "zzz"}) {
            check("v={nested=null,nested=${payload},sibling=${payload}}\npayload={x=1}\n"
                    + "a={nested={helper=0}}\na=${v}\n" + observer + "=${a.nested}\n",
                    "v={nested={x=1},sibling={x=1}}\npayload={x=1}\n"
                    + "a={nested={helper=0,x=1},sibling={x=1}}\n" + observer + "={helper=0,x=1}");
        }
        Config partial = ConfigFactory.parseString(
                "v={nested=null,nested=${pending}}\na={nested={helper=0}}\na=${v}")
                .resolve(ConfigResolveOptions.noSystem().setAllowUnresolved(true));
        partial = partial.resolve(ConfigResolveOptions.noSystem().setAllowUnresolved(true));
        Config known = ConfigFactory.parseString("v={known=3,unknown=${pending}}\na=${v}")
                .resolve(ConfigResolveOptions.noSystem().setAllowUnresolved(true));
        if (known.getInt("a.known") != 3) throw new AssertionError("ordinary partial object changed");
        Config complete = partial.withFallback(ConfigFactory.parseString("pending={x=1}"))
                .resolve(ConfigResolveOptions.noSystem());
        if (!complete.root().unwrapped().equals(ConfigFactory.parseString(
                "v={nested={x=1}}\na={nested={helper=0,x=1}}\npending={x=1}").root().unwrapped()))
            throw new AssertionError("partial resolution lost receiver history: " + complete.root().render());

    }
}
