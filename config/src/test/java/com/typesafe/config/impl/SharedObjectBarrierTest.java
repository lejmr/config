package com.typesafe.config.impl;

import com.typesafe.config.*;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public final class SharedObjectBarrierTest {
    private static SimpleConfigObject pair(SimpleConfigObject shared) {
        Map<String, ConfigValue> children = new HashMap<String, ConfigValue>();
        children.put("left", shared);
        children.put("right", shared);
        return (SimpleConfigObject) ConfigValueFactory.fromMap(children);
    }

    @Test
    public void barrierTraversalPreservesSharedSubtrees() {
        SimpleConfigObject leaf = (SimpleConfigObject) ConfigFactory.parseString(
                "leaf=null\nleaf={value=1}").getObject("leaf");
        SimpleConfigObject source = pair(pair(leaf));
        SimpleConfigObject cleaned = source.withoutFallbackBarriers();
        SimpleConfigObject node = cleaned;
        for (int depth = 0; depth < 2; depth++) {
            if (node.get("left") != node.get("right"))
                throw new AssertionError("barrier cleanup duplicated a shared subtree");
            node = (SimpleConfigObject) node.get("left");
        }
        if (node.ignoresFallbacks() || !leaf.ignoresFallbacks() || node.toConfig().getInt("value") != 1)
            throw new AssertionError("cleanup changed the source or lost the leaf value");
        SimpleConfigObject shared = cleaned;
        for (int depth = 0; depth < 40; depth++)
            shared = pair(shared);
        if (shared.hasUnresolvedFallbackBarrier() || shared.withoutFallbackBarriers() != shared)
            throw new AssertionError("barrier-free shared DAG changed");
        SimpleConfigObject unresolved = (SimpleConfigObject) ConfigFactory.parseString(
                "nested=null\nnested=${pending}").root();
        if (!pair(unresolved).hasUnresolvedFallbackBarrier())
            throw new AssertionError("shared unresolved barrier was missed");
    }
}
