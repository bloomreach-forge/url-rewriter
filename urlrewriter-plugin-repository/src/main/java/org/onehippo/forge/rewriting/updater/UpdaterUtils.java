package org.onehippo.forge.rewriting.updater;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.updater.UpdaterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class UpdaterUtils {

    private static final Logger log = LoggerFactory.getLogger(UpdaterUtils.class);


    /**
     * Removes {@link javax.jcr.Node} if it exists.
     */
    public static void removeNode(Node node, String name) throws RepositoryException {
        log.info("Removing subnode '" + name + "' from " + node.getPath() + " (exists=" + node.hasNode(name) + ")");
        if (node.hasNode(name)) {
            node.getNode(name).remove();
        }
    }


    public static void removeNode(Node node) throws RepositoryException {
        log.info("Removing node {} ", node.getPath());
        node.remove();
    }

    public static void changePrimaryType(Node node, String newPrimaryType) throws RepositoryException {
        log.info("Changing node type from {} to {} for node {}", new Object[]{node.getPrimaryNodeType().getName(), newPrimaryType, node.getPath()});
        ((UpdaterNode) node).setPrimaryNodeType(newPrimaryType);
    }

    public static void updateContentToNewUrlRewriterTypes(Node node) throws RepositoryException {

        if (node.isNodeType("hippo:handle")) {
            //Handle preview + live children
            for (NodeIterator rulesetNodes = node.getNodes(node.getName()); rulesetNodes.hasNext(); ) {
                Node rulesetNode = rulesetNodes.nextNode();

                if (rulesetNode != null && rulesetNode.isNodeType("urlrewriter:ruleset")) {
                    log.info("processing ruleset {}", rulesetNode.getPath());

                    changePrimaryType(rulesetNode, "urlrewriter:advancedrule");

                    //TODO We should create a new advanced rule for every rule we find.
                    if (rulesetNode.getNodes("urlrewriter:rule").getSize() > 1) {
                        log.warn("Rewriter rule has many rule compounds {}", rulesetNode.getPath());
                    }

                    //TODO Instead, we deal with the first we find
                    Node oldRewriterRule = rulesetNode.getNode("urlrewriter:rule");

                    if (oldRewriterRule != null) {
                        rulesetNode.setProperty("urlrewriter:casesensitive", oldRewriterRule.getProperty("urlrewriter:casesensitive").getBoolean());
                        rulesetNode.setProperty("urlrewriter:ruledescription", oldRewriterRule.getProperty("urlrewriter:rulename").getString());
                        rulesetNode.setProperty("urlrewriter:rulefrom", oldRewriterRule.getProperty("urlrewriter:rulefrom").getString());
                        rulesetNode.setProperty("urlrewriter:ruleto", oldRewriterRule.getProperty("urlrewriter:ruleto").getString());
                        rulesetNode.setProperty("urlrewriter:ruletype", oldRewriterRule.getProperty("urlrewriter:ruletype").getString());
                        rulesetNode.setProperty("urlrewriter:iswildcardtype", "false");
                        rulesetNode.setProperty("urlrewriter:isnotlast", "false");

                        for (NodeIterator oldRuleConditions = oldRewriterRule.getNodes("urlrewriter:rulecondition"); oldRuleConditions.hasNext(); ) {
                            Node oldRuleCondition = oldRuleConditions.nextNode();
                            if (oldRuleCondition.hasProperty("conditionpredefinedname")) {
                                log.warn("Old rewriter rule condition has property conditionpredefinedname. This is not mapped. Rule: {}, condition: {}", oldRewriterRule.getPath(), oldRuleCondition.getPath());
                            }
                            log.info("Copying rulecondition {}", oldRuleCondition.getPath());

                            Node newRuleCondition = rulesetNode.addNode("urlrewriter:rulecondition", "urlrewriter:rulecondition");
                            newRuleCondition.setProperty("urlrewriter:conditionname", oldRuleCondition.getProperty("urlrewriter:conditionname").getString());
                            newRuleCondition.setProperty("urlrewriter:conditionoperator", oldRuleCondition.getProperty("urlrewriter:conditionoperator").getString());
                            newRuleCondition.setProperty("urlrewriter:conditiontype", oldRuleCondition.getProperty("urlrewriter:conditiontype").getString());
                            newRuleCondition.setProperty("urlrewriter:conditionor", oldRuleCondition.getProperty("urlrewriter:conditionor").getString());
                            newRuleCondition.setProperty("urlrewriter:conditionvalue", oldRuleCondition.getProperty("urlrewriter:conditionvalue").getString());
                        }
                    }
                    log.info("finished processing ruleset {}", rulesetNode.getPath());
                }
            }

        } else if (node.isNodeType("hippostd:directory") || node.isNodeType("hippostd:folder")) {
            log.info("processing directory {}", node.getPath());
            for (NodeIterator children = node.getNodes(); children.hasNext(); ) {
                Node childNode = children.nextNode();
                try {
                    updateContentToNewUrlRewriterTypes(childNode);
                } catch (RepositoryException e) {
                    log.error("Exception while processing node {}", childNode.getPath());
                }
            }
            log.info("finished processing directory {}", node.getPath());
        }
    }

    public static void removeOldRewriterRules(Node node) throws RepositoryException {
        if (node.isNodeType("hippo:handle")) {
            //Handle preview + live children
            for (NodeIterator advancedRuleNodes = node.getNodes(node.getName()); advancedRuleNodes.hasNext(); ) {
                Node advancedRuleNode = advancedRuleNodes.nextNode();

                if (advancedRuleNode != null && advancedRuleNode.isNodeType("urlrewriter:advancedrule")) {
                    log.info("processing advancedrule {}", advancedRuleNode.getPath());
                    for (NodeIterator oldRewriterRules = advancedRuleNode.getNodes("urlrewriter:rule"); oldRewriterRules.hasNext(); ) {
                        Node oldRewriterRule = oldRewriterRules.nextNode();
                        try {
                            removeNode(oldRewriterRule);
                        } catch (RepositoryException e) {
                            log.error("Exception while processing node {}", oldRewriterRule.getPath());
                        }
                    }
                    log.info("finished processing advancedrule {}", advancedRuleNode.getPath());
                }
            }
        } else if (node.isNodeType("hippostd:directory") || node.isNodeType("hippostd:folder")) {
            log.info("processing directory {}", node.getPath());
            for (NodeIterator children = node.getNodes(); children.hasNext(); ) {
                removeOldRewriterRules(children.nextNode());
            }
            log.info("finished processing directory {}", node.getPath());
        }
    }

    public static void changeDirectoriesToRulesets(Node node) throws RepositoryException {
        if (node.isNodeType("hippostd:directory") || node.isNodeType("hippostd:folder")) {
            log.info("processing directory {}", node.getPath());
            for (NodeIterator children = node.getNodes(); children.hasNext(); ) {
                changeDirectoriesToRulesets(children.nextNode());
            }

            try {
                changePrimaryType(node, "urlrewriter:ruleset");
            } catch (RepositoryException e) {
                log.error("Exception while processing node {}", node.getPath());
            }

            log.info("finished processing directory {}", node.getPath());
        }
    }


}

