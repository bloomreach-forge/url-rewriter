/**
 * Copyright (C) 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.rewriting.repo;

import java.util.Date;
import java.util.List;

import javax.jcr.observation.Event;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.onehippo.forge.rewriting.UrlRewriteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 * @version $Id$
 */
public class RewritingManager {

    private static Logger log = LoggerFactory.getLogger(RewritingManager.class);

    public static final String RULES_PRIORITY_COMPLEX_FIRST = "complexRulesFirst";
    private static final String RULES_PRIORITY_SIMPLE_FIRST = "simpleRulesFirst";
    public static final String DISABLED_RULE_TYPES_COMPLEX = "complex";
    public static final String DISABLED_RULE_TYPES_SIMPLE = "simple";

    // spring managed
    private RewritingRulesExtractor complexRulesExtractor;
    private RewritingRulesExtractor simpleRulesExtractor;

    // default, no rules
    private StringBuilder loadedRules = new StringBuilder();
    private Date lastLoadDate = new Date();

    private volatile boolean needRefresh = true;

    public boolean needReloading() {
        return needRefresh;
    }

    /**
     * Load rules for given path. If no path is provided, default will be used (configured in spring configuration)
     *
     * @param context
     * @param request
     * @param urlRewriteLocation absolute repository path
     */
    @Deprecated
    public synchronized StringBuilder loadRules(final ServletContext context, final ServletRequest request, final String urlRewriteLocation) {
        return loadRules(context, request, null, null, null, null);
    }


    /**
    * Load rules for given path. If no path is provided, default will be used (configured in spring configuration)
    *
    * @param context
    * @param request
    * @param complexRewriteRulesLocation absolute repository path of the complex rules (ruleset/rulesetxml documents)
    * @param simpleRewriteRulesLocation absolute repository path of the simple rules (simplerule documents)
    * @param rulesPriority Specify if complex or simple rules should be handled first (complexRulesFirst|simpleRulesFirst)
    * @param disabledRuleTypes List of rule types that are disabled (values: complex|simple)
    */
    public synchronized StringBuilder loadRules(final ServletContext context, final ServletRequest request, final String complexRewriteRulesLocation, final String simpleRewriteRulesLocation, final String rulesPriority, final List<String> disabledRuleTypes) {
        // check if refresh is needed..if not return local copy
        if (!needRefresh) {
            return loadedRules;
        }

        //Get complex rules
        StringBuilder complexRules = new StringBuilder();
        if(disabledRuleTypes == null || !disabledRuleTypes.contains(DISABLED_RULE_TYPES_COMPLEX)){
            if(StringUtils.isBlank(complexRewriteRulesLocation)){
                log.debug("Filter configuration does not specify UrlRewriteLocation, or is not an absolute path. Will use default one: {}", complexRulesExtractor.getRewriteRulesLocation());
            } else {
                complexRulesExtractor.setRewriteRulesLocation(complexRewriteRulesLocation);
            }
            complexRules.append(complexRulesExtractor.load(context, request));
        }

        //Get simple rules
        StringBuilder simpleRules = new StringBuilder();
        if(disabledRuleTypes == null || !disabledRuleTypes.contains(DISABLED_RULE_TYPES_SIMPLE)){
            if(StringUtils.isBlank(simpleRewriteRulesLocation)){
                log.debug("Filter configuration does not specify UrlRewriteLocation, or is not an absolute path. Will use default one: {}", simpleRulesExtractor.getRewriteRulesLocation());
            } else {
                simpleRulesExtractor.setRewriteRulesLocation(simpleRewriteRulesLocation);
            }
            simpleRules.append(simpleRulesExtractor.load(context, request));
        }

        //Finally fill up the rules with the correct order
        StringBuilder rules = new StringBuilder(UrlRewriteConstants.XML_START);
        if(RULES_PRIORITY_COMPLEX_FIRST.equals(rulesPriority) || StringUtils.isBlank(rulesPriority)){
            rules.append(complexRules).append(simpleRules);
        } else if(RULES_PRIORITY_SIMPLE_FIRST.equals(rulesPriority)){
            rules.append(simpleRules).append(complexRules);
        }
        rules.append(UrlRewriteConstants.XML_END);

        //Update our state
        loadedRules = new StringBuilder(rules);
        needRefresh = false;
        lastLoadDate = new Date();

        return rules;
    }



    public RewritingRulesExtractor getComplexRulesExtractor() {
        return complexRulesExtractor;
    }

    public void setComplexRulesExtractor(final RewritingRulesExtractor complexRulesExtractor) {
        this.complexRulesExtractor = complexRulesExtractor;
    }

    public RewritingRulesExtractor getSimpleRulesExtractor() {
        return simpleRulesExtractor;
    }

    public void setSimpleRulesExtractor(final RewritingRulesExtractor simpleRulesExtractor) {
        this.simpleRulesExtractor = simpleRulesExtractor;
    }


    public void invalidate(final Event event) {
        needRefresh = true;
    }

    public Date getLastLoadDate() {
        return lastLoadDate;
    }
}
