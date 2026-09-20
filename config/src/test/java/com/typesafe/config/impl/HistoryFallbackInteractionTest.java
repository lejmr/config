package com.typesafe.config.impl;

import com.typesafe.config.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** Supplemental one-pass/two-pass invariant checks, not independent conformance proofs. */
public final class HistoryFallbackInteractionTest {
    private static Object resolve(Config source) {
        try {
            return source.resolve(ConfigResolveOptions.noSystem()).root().unwrapped();
        } catch (ConfigException e) {
            return e.getClass().getName();
        }
    }

    @Test
    public void partialThenCompleteResolutionMatchesOnePass() {
        String[] cases = {
            "v={nested=null,nested=${pending}}\na={nested={helper=0}}\na=${v}",
            "v={nested=null,nested=${pending}}\na={nested={helper=0}}\na=${v}\nb=${a}",
            "a={nested=null,nested=${pending}}\na=${?MISSING}${a}",
            "a={v={nested=null,nested=${pending}}}\na=${a.v}",
            "a=null\na {services=${pending}\n copy=${a.services}}",
            "a={p=0,v=${a.p}}\na=${?MISSING}[${a.p}]",
            "wrapper={old=${MISSING}}\nwrapper=${pending}",
            "v={nested=null,nested=${pending}}\na={nested={helper=0}}\na=${v}\nb=${a.nested}"
        };
        for (String source : cases) {
            for (String pending : new String[] {"{x=1}", "42", "null"}) {
                Config base = ConfigFactory.parseString(source);
                Config added = ConfigFactory.parseString("pending=" + pending);
                Object onePass = resolve(base.withFallback(added));
                Object twoPass;
                try {
                    Config partial = base.resolve(ConfigResolveOptions.noSystem().setAllowUnresolved(true));
                    twoPass = resolve(partial.withFallback(added));
                } catch (ConfigException e) {
                    twoPass = e.getClass().getName();
                }
                assertEquals("source=" + source + ", pending=" + pending, onePass, twoPass);
            }
        }
    }
}
