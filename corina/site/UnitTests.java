//
// This file is part of Corina.
//
// Corina is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// Corina is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Corina; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Copyright 2001 Ken Harris <kbh7@cornell.edu>
//

package corina.site;

import junit.framework.TestCase;

public class UnitTests extends TestCase {
    public UnitTests(String name) {
        super(name);
    }

    //
    // testing SiteDB.java
    //
    public void testSiteDB() {
        if (System.getProperty("org.xml.sax.driver") == null)
            System.setProperty("org.xml.sax.driver", "gnu.xml.aelfred2.SAXDriver");
        if (System.getProperty("corina.sites.file") == null)
            System.setProperty("corina.sites.file", "sites.db");
        try {
            SiteDB db = SiteDB.getSiteDB();
            assert(db != null);
            assert(db.sites != null);
        } catch (Exception e) {
            fail();
        }
    }
}
