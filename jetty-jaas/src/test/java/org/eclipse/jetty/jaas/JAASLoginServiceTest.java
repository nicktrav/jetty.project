//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//


package org.eclipse.jetty.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Principal;

import javax.security.auth.Subject;

import org.junit.Test;

/**
 * JAASLoginServiceTest
 *
 *
 */
public class JAASLoginServiceTest
{
    interface SomeRole
    {
        
    }

    public class TestRole implements Principal, SomeRole
    {
        String _name;

        public TestRole (String name)
        {
            _name = name;
        }

        public String getName()
        {
            return _name;    
        }
    }
    
    
    public class AnotherTestRole extends TestRole
    {
        public AnotherTestRole(String name)
        {
            super(name);
        }   
    }
    
    public class NotTestRole implements Principal
    {
        String _name;
        
        public NotTestRole (String n)
        {
            _name = n;
        }
        
        public String getName()
        {
            return _name;    
        }
    }
    
    

    @Test
    public void testLoginServiceRoles () throws Exception
    {
        JAASLoginService ls = new JAASLoginService("foo");
        
        //test that we always add in the DEFAULT ROLE CLASSNAME
        ls.setRoleClassNames(new String[] {"arole", "brole"});
        String[] roles = ls.getRoleClassNames();
        assertEquals(3, roles.length);
        assertEquals(JAASLoginService.DEFAULT_ROLE_CLASS_NAME, roles[2]);
        
        ls.setRoleClassNames(new String[] {});
        assertEquals(1, ls.getRoleClassNames().length);
        assertEquals(JAASLoginService.DEFAULT_ROLE_CLASS_NAME, ls.getRoleClassNames()[0]);

        ls.setRoleClassNames(null);
        assertEquals(1, ls.getRoleClassNames().length);
        assertEquals(JAASLoginService.DEFAULT_ROLE_CLASS_NAME, ls.getRoleClassNames()[0]);

        //test a custom role class where some of the roles are subclasses of it
        ls.setRoleClassNames(new String[] {TestRole.class.getName()});
        Subject subject = new Subject();
        subject.getPrincipals().add(new NotTestRole("w"));
        subject.getPrincipals().add(new TestRole("x"));
        subject.getPrincipals().add(new TestRole("y"));
        subject.getPrincipals().add(new AnotherTestRole("z"));
        
        String[] groups = ls.getGroups(subject);
        assertEquals(3, groups.length);
        for (String g:groups)
            assertTrue(g.equals("x") || g.equals("y") || g.equals("z"));
        
        //test a custom role class
        ls.setRoleClassNames(new String[] {AnotherTestRole.class.getName()});
        Subject subject2 = new Subject();
        subject2.getPrincipals().add(new NotTestRole("w"));
        subject2.getPrincipals().add(new TestRole("x"));
        subject2.getPrincipals().add(new TestRole("y"));
        subject2.getPrincipals().add(new AnotherTestRole("z"));
        assertEquals(1, ls.getGroups(subject2).length);
        assertEquals("z", ls.getGroups(subject2)[0]);
        
        //test a custom role class that implements an interface
        ls.setRoleClassNames(new String[] {SomeRole.class.getName()});
        Subject subject3 = new Subject();
        subject3.getPrincipals().add(new NotTestRole("w"));
        subject3.getPrincipals().add(new TestRole("x"));
        subject3.getPrincipals().add(new TestRole("y"));
        subject3.getPrincipals().add(new AnotherTestRole("z"));
        assertEquals(3, ls.getGroups(subject3).length);
        for (String g:groups)
            assertTrue(g.equals("x") || g.equals("y") || g.equals("z"));
        
        //test a class that doesn't match
        ls.setRoleClassNames(new String[] {NotTestRole.class.getName()});
        Subject subject4 = new Subject();
        subject4.getPrincipals().add(new TestRole("x"));
        subject4.getPrincipals().add(new TestRole("y"));
        subject4.getPrincipals().add(new AnotherTestRole("z"));
        assertEquals(0, ls.getGroups(subject4).length);       
    }

}
