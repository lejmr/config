package com.typesafe.config.impl;

import com.typesafe.config.*;
import org.junit.Test;
import com.typesafe.config.parser.ConfigDocumentFactory;
import java.util.Arrays;

public class LeadingDotNumberTest {
    @Test
    public void requiresIntegerPartForNegativeNumbers() {
        for (ConfigSyntax syntax : new ConfigSyntax[] {ConfigSyntax.CONF, ConfigSyntax.JSON}) {
            for (String literal : new String[] {"-.33", "-.33e+1", "-.33E-1"}) {
                try {
                    ConfigFactory.parseString("{\"a\":" + literal + "}", ConfigParseOptions.defaults().setSyntax(syntax));
                    throw new AssertionError("accepted missing integer part: " + syntax + " " + literal);
                } catch (ConfigException.Parse expected) { }
                try {
                    ConfigDocumentFactory.parseString("{\"a\":" + literal + "}",
                            ConfigParseOptions.defaults().setSyntax(syntax));
                    throw new AssertionError("document accepted missing integer part: " + literal);
                } catch (ConfigException.Parse expected) { }
            }
        }
        Config c = ConfigFactory.parseString("a=-0.33\nb=-0.33e+1\nc=-1e-2\nq=\"-.33e+1\"\ns=1e+2foo\nu=.33");
        if (c.getDouble("a") != -0.33 || c.getDouble("b") != -3.3 || c.getDouble("c") != -0.01)
            throw new AssertionError("valid decimals/exponents");
        if (!c.getString("q").equals("-.33e+1") || !c.getString("s").equals("1e+2foo") || !c.getString("u").equals(".33"))
            throw new AssertionError("string controls");
        Config legacy = ConfigFactory.parseString("a=-.foo\nb=-.33.4\n-.foo=7");
        if (!legacy.getString("a").equals("-.foo") || !legacy.getString("b").equals("-.33.4")
                || legacy.getInt("-.foo") != 7 || !ConfigUtil.splitPath("-.foo").toString().equals("[-, foo]"))
            throw new AssertionError("non-numeric token and path controls");

    }
    @Test
    public void numericLookingPathsRemainUsable() {
        String path = ConfigUtil.joinPath("-", "33");
        if (!ConfigUtil.splitPath(path).equals(Arrays.asList("-", "33")))
            throw new AssertionError("generated path must split into its original keys");
        Config quoted = ConfigFactory.parseString("\"-\" {\"33\"=7}");
        if (quoted.getInt(path) != 7) throw new AssertionError("generated path cannot read its value");
        Config literal = ConfigFactory.parseString("-.33=7\ncopy=${-.33}").resolve();
        if (literal.getInt(path) != 7 || literal.getInt("copy") != 7)
            throw new AssertionError("literal and substitution paths must retain their meaning");
        String document = "-.33=7";
        if (!ConfigDocumentFactory.parseString(document).render().equals(document))
            throw new AssertionError("document parser changed a valid path");
        for (String value : new String[] {"-.33", "[-.33]", "{a=-.33}", "-.33 suffix"}) {
            try {
                ConfigDocumentFactory.parseString("a=1").withValueText("a", value);
                throw new AssertionError("document edit accepted invalid numeric value: " + value);
            } catch (ConfigException.Parse expected) { }
        }
    }

}
