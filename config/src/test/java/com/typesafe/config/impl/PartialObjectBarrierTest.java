package com.typesafe.config.impl;

import com.typesafe.config.*;
import org.junit.Test;

public final class PartialObjectBarrierTest {
    private static Config resolve(String source) {
        return ConfigFactory.parseString(source).resolve(
                ConfigResolveOptions.defaults().setUseSystemEnvironment(false));
    }

    @Test
    public void keepsHigherPriorityChildrenAcrossBarrier() {
        for (String prior : new String[] { "null", "0", "false", "\"text\"", "[]", "[${MISSING}]" }) {
            for (String optional : new String[] { "", "?" }) {
                Config c = resolve("a=" + prior + "\na { services={x=1}\n nested.services=${"
                        + optional + "a.services} }");
                if (c.getInt("a.nested.services.x") != 1) throw new AssertionError(c.root().render());
            }
        }
        Config merged = resolve("a=null\na={services={x=1}}\na {services={y=2}\n copy=${a.services}}");
        if (merged.getInt("a.copy.x") != 1 || merged.getInt("a.copy.y") != 2)
            throw new AssertionError(merged.root().render());
        for (String source : new String[] {
                "a {services={x=1}\n nested.services=${a.services}}",
                "a={}\na {services={x=1}\n nested.services=${a.services}}",
                "a=null\na {nested.services=${a.services}\n services={x=1}}",
                "a=null\na.services={x=1}\na.nested.services=${a.services}",
                "v={x=1}\na=null\na {services=${v}\n nested.services=${a.services}}",
                "v=null\na=${v}\na {services={x=1}\n nested.services=${a.services}}",
                "a={old=1}\na=null\na {services={x=1}\n nested.services=${a.services}}",
                "a=null\na {services=null\n services={x=1}\n nested.services=${a.services}}" }) {
            Config c = resolve(source);
            if (c.getInt("a.nested.services.x") != 1) throw new AssertionError(c.root().render());
        }
        Config deep = resolve("z=null\nz {v={x=1}\n deeper.child=${z.v.x}}");
        if (deep.getInt("z.deeper.child") != 1) throw new AssertionError(deep.root().render());
        Config omitted = resolve("a=null\na {copy=${?a.absent}}");
        if (omitted.hasPathOrNull("a.copy")) throw new AssertionError(omitted.root().render());
        resolve("a=null\na {services=${MISSING}\n copy=${a.services}}\na=42");
        for (String child : new String[] { "7", "[]", "null" }) {
            Config c = resolve("a=null\na { services=" + child + "\n copy=${a.services} }");
            ConfigObject a = c.getObject("a");
            if (!a.get("services").equals(a.get("copy"))) throw new AssertionError(a.render());
        }
        for (String source : new String[] {
                "a=null\na { services=${MISSING}\n copy=${a.services} }",
                "a=null\na { services=${a.copy}\n copy=${a.services} }" }) {
            try {
                resolve(source);
                throw new AssertionError("expected unresolved substitution: " + source);
            } catch (ConfigException.UnresolvedSubstitution expected) {
            }
        }

    }
}
