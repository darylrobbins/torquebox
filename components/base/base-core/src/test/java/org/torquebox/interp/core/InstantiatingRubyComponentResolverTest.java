/*
 * Copyright 2008-2011 Red Hat, Inc, and individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.torquebox.interp.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.junit.Before;
import org.junit.Test;

public class InstantiatingRubyComponentResolverTest {

    private Ruby ruby;

    @Before
    public void setUpRuby() throws Exception {
        List<String> loadPaths = new ArrayList<String>();
        this.ruby = JavaEmbedUtils.initialize( loadPaths );
    }

    /** Ensure that resolution instantiates if-required. */
    @Test
    public void testResolveToInstantiate() throws Exception {
        InstantiatingRubyComponentResolver resolver = new InstantiatingRubyComponentResolver();

        this.ruby.evalScriptlet( "class ComponentClass; end" );
        resolver.setComponentName( "component-foo" );
        resolver.setRubyClassName( "ComponentClass" );

        IRubyObject component = resolver.resolve( this.ruby );

        assertNotNull( component );
        assertEquals( "ComponentClass", component.getMetaClass().getName() );
    }

    /** Ensure that repeated resolutions resolve to the same object. */
    @Test
    public void testResolveRepeatedly() throws Exception {
        InstantiatingRubyComponentResolver resolver = new InstantiatingRubyComponentResolver();

        this.ruby.evalScriptlet( "class ComponentClass; end" );
        resolver.setComponentName( "component-foo" );
        resolver.setRubyClassName( "ComponentClass" );

        IRubyObject component = resolver.resolve( this.ruby );
        assertNotNull( component );
        assertEquals( "ComponentClass", component.getMetaClass().getName() );

        IRubyObject componentToo = resolver.resolve( this.ruby );
        assertNotNull( componentToo );

        assertSame( component, componentToo );
    }

    /** Ensure that appropriate file is required/loaded if provided. */
    @Test
    public void testResolveWithRequirePath() throws Exception {
        InstantiatingRubyComponentResolver resolver = new InstantiatingRubyComponentResolver();

        resolver.setComponentName( "some-component" );
        resolver.setRubyClassName( "SomeComponent" );
        resolver.setRubyRequirePath( "org/torquebox/interp/core/some_component" );

        IRubyObject component = resolver.resolve( this.ruby );
        assertNotNull( component );

        assertEquals( "SomeComponent", component.getMetaClass().getName() );

        IRubyObject componentToo = resolver.resolve( this.ruby );
        assertNotNull( componentToo );

        assertSame( component, componentToo );
    }

    /** Ensure that constructors may take arguments. */
    @Test
    public void testResolveWithContructorArguments() throws Exception {
        InstantiatingRubyComponentResolver resolver = new InstantiatingRubyComponentResolver();

        resolver.setComponentName( "optional-component" );
        resolver.setRubyClassName( "OptionalComponent" );
        resolver.setRubyRequirePath( "org/torquebox/interp/core/optional_component" );
        Map options = ruby.evalScriptlet( "{ :a => '1', 'b' => '2' }" ).convertToHash();
        resolver.setInitializeParams( options );

        IRubyObject component = resolver.resolve( this.ruby );
        assertNotNull( component );
        assertEquals( "1", JavaEmbedUtils.invokeMethod( this.ruby, component, "[]", new Object[] { ruby.evalScriptlet( ":a" ) }, String.class ) );
        assertEquals( "2", JavaEmbedUtils.invokeMethod( this.ruby, component, "[]", new Object[] { "b" }, String.class ) );
    }

    /**
     * Ensure that multiple resolvers keep their components distinct within an
     * interpreter.
     */
    @Test
    public void testResolveMultipleNames() throws Exception {
        InstantiatingRubyComponentResolver resolverOne = new InstantiatingRubyComponentResolver();
        resolverOne.setComponentName( "component-one" );
        resolverOne.setRubyClassName( "ComponentClassOne" );

        InstantiatingRubyComponentResolver resolverTwo = new InstantiatingRubyComponentResolver();
        resolverTwo.setComponentName( "component-two" );
        resolverTwo.setRubyClassName( "ComponentClassTwo" );

        this.ruby.evalScriptlet( "class ComponentClassOne; end" );
        this.ruby.evalScriptlet( "class ComponentClassTwo; end" );

        IRubyObject componentOne = resolverOne.resolve( this.ruby );
        assertNotNull( componentOne );
        assertEquals( "ComponentClassOne", componentOne.getMetaClass().getName() );

        IRubyObject componentTwo = resolverTwo.resolve( this.ruby );
        assertNotNull( componentTwo );
        assertEquals( "ComponentClassTwo", componentTwo.getMetaClass().getName() );
    }

    /**
     * Ensure that repeated resolutions resolve to different objects when always
     * reloading.
     */
    @Test
    public void testAlwaysReload() throws Exception {
        InstantiatingRubyComponentResolver resolver = new InstantiatingRubyComponentResolver();

        this.ruby.evalScriptlet( "class ComponentClass; end" );
        resolver.setComponentName( "component-foo" );
        resolver.setRubyClassName( "ComponentClass" );
        resolver.setAlwaysReload( true );

        IRubyObject component = resolver.resolve( this.ruby );
        assertNotNull( component );
        assertEquals( "ComponentClass", component.getMetaClass().getName() );

        IRubyObject componentToo = resolver.resolve( this.ruby );
        assertNotNull( componentToo );

        assertNotSame( component, componentToo );
    }

}
