/*
 * HibernateRollerPlanetManagerImpl.java
 *
 * Created on April 17, 2006, 1:53 PM
 */

package org.roller.business.hibernate;

import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.roller.RollerException;
import org.roller.config.RollerRuntimeConfig;
import org.roller.model.PagePluginManager;
import org.roller.model.Roller;
import org.roller.model.RollerFactory;
import org.roller.model.UserManager;
import org.roller.model.WeblogManager;
import org.roller.pojos.PlanetEntryData;
import org.roller.pojos.PlanetSubscriptionData;
import org.roller.pojos.WeblogEntryData;
import org.roller.pojos.WebsiteData;
import org.roller.util.StringUtils;


/**
 * An extended version of the base PlanetManager implementation.
 * 
 * This is meant for use by Roller installations that are running the planet
 * aggregator in the same application instance and want to fetch feeds from
 * their local Roller blogs in a more efficient manner.
 */
public class HibernateRollerPlanetManagerImpl extends HibernatePlanetManagerImpl {
    
    private static Log log = LogFactory.getLog(HibernateRollerPlanetManagerImpl.class);
    
    
    public HibernateRollerPlanetManagerImpl(HibernatePersistenceStrategy strat) {
        
        super(strat);
        
        log.info("Instantiating Hibernate Roller Planet Manager");
    }
    
    
    protected Set getNewEntries(PlanetSubscriptionData sub,
                                FeedFetcher feedFetcher,
                                FeedFetcherCache feedInfoCache)
            throws RollerException {
        
        String localURL = RollerRuntimeConfig.getProperty("site.absoluteurl");
        
        // if this is not a local url then let parent deal with it
        if (StringUtils.isEmpty(localURL) || !sub.getFeedUrl().startsWith(localURL)) {
            
            log.debug("Feed is remote, letting parent handle it "+sub.getFeedUrl());
            
            return super.getNewEntries(sub, feedFetcher, feedInfoCache);
        }
        
        // url must be local, lets deal with it
        Set newEntries = new TreeSet();
        try {
            // for local feeds, sub.author = website.handle
            if (sub.getAuthor()!=null && sub.getFeedUrl().endsWith(sub.getAuthor())) {
                
                log.debug("Getting LOCAL feed "+sub.getFeedUrl());
                
                // get corresponding website object
                UserManager usermgr = RollerFactory.getRoller().getUserManager();
                WebsiteData website = usermgr.getWebsiteByHandle(sub.getAuthor());
                if (website == null) return newEntries;
                
                // figure website last update time
                WeblogManager blogmgr = RollerFactory.getRoller().getWeblogManager();
                
                Date siteUpdated = blogmgr.getWeblogLastPublishTime(website);
                if (siteUpdated == null) { // Site never updated, skip it
                    log.warn("Last-publish time null, skipping local feed ["
                            + website.getHandle() + "]");
                    return newEntries;
                }
                
                // if website last update time > subsciption last update time
                List entries = new ArrayList();
                if (sub.getLastUpdated()==null || siteUpdated.after(sub.getLastUpdated())) {
                    int entryCount = RollerRuntimeConfig.getIntProperty(
                            "site.newsfeeds.defaultEntries");
                    entries = blogmgr.getWeblogEntries(
                            website,
                            null,                        // startDate
                            new Date(),                  // endDate
                            null,                        // catName
                            WeblogEntryData.PUBLISHED,   // status
                            null,                        // sortby (null means pubTime)
                            new Integer(entryCount));    // maxEntries
                    
                    sub.setLastUpdated(siteUpdated);
                    saveSubscription(sub);
                    
                } else {
                    if (log.isDebugEnabled()) {
                        String msg = MessageFormat.format(
                                "   Skipping ({0} / {1})", new Object[] {
                            siteUpdated, sub.getLastUpdated()});
                        log.debug(msg);
                    }
                }
                
                // Populate subscription object with new entries
                PagePluginManager ppmgr = RollerFactory.getRoller().getPagePluginManager();
                Map pagePlugins = ppmgr.createAndInitPagePlugins(
                        website,
                        null,
                        RollerRuntimeConfig.getProperty("site.absoluteurl"),
                        new VelocityContext());
                Iterator entryIter = entries.iterator();
                while (entryIter.hasNext()) {
                    try {
                        WeblogEntryData rollerEntry =
                                (WeblogEntryData)entryIter.next();
                        PlanetEntryData entry =
                                new PlanetEntryData(rollerEntry, sub, pagePlugins);
                        saveEntry(entry);
                        newEntries.add(entry);
                    } catch (Exception e) {
                        log.error("ERROR processing subscription entry", e);
                    }
                }
                return newEntries;
            }
        } catch (Exception e) {
            log.warn("Problem reading local feed", e);
        }
        
        log.debug("Failed to fetch locally, trying remote "+sub.getFeedUrl());
        
        // if there was an error then try normal planet method
        return super.getNewEntries(sub, feedFetcher, feedInfoCache);
    }
    
}