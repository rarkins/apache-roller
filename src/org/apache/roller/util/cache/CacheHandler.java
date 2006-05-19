/*
* Licensed to the Apache Software Foundation (ASF) under one or more
*  contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*/

package org.apache.roller.util.cache;

import java.util.Map;
import org.apache.roller.pojos.BookmarkData;
import org.apache.roller.pojos.CommentData;
import org.apache.roller.pojos.FolderData;
import org.apache.roller.pojos.RefererData;
import org.apache.roller.pojos.UserData;
import org.apache.roller.pojos.WeblogCategoryData;
import org.apache.roller.pojos.WeblogEntryData;
import org.apache.roller.pojos.WeblogTemplate;
import org.apache.roller.pojos.WebsiteData;


/**
 * A class which utilizes a cache.
 *
 * The primary purpose of this interface is to force cache handlers to implement
 * the set of invalidate() methods which server as notifications of changed
 * objects.  Various caches can determine for themselves how to deal with changes
 * to each type of object.
 */
public interface CacheHandler {
    
    public void invalidate(WeblogEntryData entry);
    
    public void invalidate(WebsiteData website);
    
    public void invalidate(BookmarkData bookmark);
    
    public void invalidate(FolderData folder);

    public void invalidate(CommentData comment);

    public void invalidate(RefererData referer);

    public void invalidate(UserData user);

    public void invalidate(WeblogCategoryData category);

    public void invalidate(WeblogTemplate template);
    
    public void clear();
    
    public Map getStats();
    
}