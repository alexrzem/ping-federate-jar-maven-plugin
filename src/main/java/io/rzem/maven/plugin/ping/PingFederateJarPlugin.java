package io.rzem.maven.plugin.ping;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.space.asm.ClassReader;

/**
 * 
 * MIT License
 * 
 * Copyright (c) 2020 Alex Rzem
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
@Mojo( name = "ping-federate-jar", defaultPhase = LifecyclePhase.COMPILE )
public class PingFederateJarPlugin extends AbstractMojo {

	private static final String PF_INF = "PF-INF";
	private static final String[] EXTENSIONS = { "class" };

	private List<PluginType> PLUGIN_TYPES;

	@Parameter( defaultValue = "${project}", required = true, readonly = true )
	private MavenProject project;

	public PingFederateJarPlugin() {
		PLUGIN_TYPES = new ArrayList<>();
		PLUGIN_TYPES.add( new PluginType( "authentication-selectors", "com.pingidentity.sdk.AdapterSelector", "com.pingidentity.sdk.AuthenticationSelector" ) );
		PLUGIN_TYPES.add( new PluginType( "bearer-access-token-management-plugins", "com.pingidentity.sdk.oauth20.BearerAccessTokenManagementPlugin" ) );
		PLUGIN_TYPES.add( new PluginType( "custom-drivers", "com.pingidentity.sources.CustomDataSourceDriver" ) );
		PLUGIN_TYPES.add( new PluginType( "dynamic-client-registration", "com.pingidentity.sdk.oauth20.registration.DynamicClientRegistrationPlugin" ) );
		PLUGIN_TYPES.add( new PluginType( "identity-store-provisioners", "com.pingidentity.sdk.provision.IdentityStoreProvisionerWithFiltering" ) );
		PLUGIN_TYPES.add( new PluginType( "idp-authn-adapters", "org.sourceid.saml20.adapter.idp.authn.IdpAuthenticationAdapter", "com.pingidentity.sdk.IdpAuthenticationAdapterV2" ) );
		PLUGIN_TYPES.add( new PluginType( "notification-sender", "com.pingidentity.sdk.notification.NotificationPublisherPlugin" ) );
		PLUGIN_TYPES.add( new PluginType( "oob-auth-plugins", "com.pingidentity.sdk.oobauth.OOBAuthPlugin" ) );
		PLUGIN_TYPES.add( new PluginType( "password-credential-validators", "com.pingidentity.sdk.password.PasswordCredentialValidator" ) );
		PLUGIN_TYPES.add( new PluginType( "saas-provisioning-plugin-descriptor", "saas-provisioning-plugin-descriptor" ) );
		PLUGIN_TYPES.add( new PluginType( "sp-authn-adapters", "org.sourceid.saml20.adapter.sp.authn.SpAuthenticationAdapter" ) );
		PLUGIN_TYPES.add( new PluginType( "token-generators", "org.sourceid.wstrust.plugin.generate.TokenGenerator" ) );
		PLUGIN_TYPES.add( new PluginType( "token-processors", "org.sourceid.wstrust.plugin.process.TokenProcessor" ) );
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		File pfInf = makePFInfDir();

		List<File> classpath = getClasspath();
		populatePluginSets( classpath );

		for ( PluginType pluginType : PLUGIN_TYPES ) {
			if ( !pluginType.classes.isEmpty() ) {
				try {
					getLog().info( pluginType.fileName + " -> " + pluginType.classes );

					StringBuilder buf = new StringBuilder();
					pluginType.classes.forEach( className -> {
						buf.append( className ).append( IOUtils.LINE_SEPARATOR );
					} );

					FileUtils.write( new File( pfInf, pluginType.fileName ), buf.toString(), StandardCharsets.UTF_8, false );
				} catch ( Throwable e ) {
					getLog().error( "Exception thrown wihile getting paths: ", e );
				}
			}
		}
	}

	@SuppressWarnings( "unchecked" )
	protected List<File> getClasspath() {
		List<File> classpath = new ArrayList<>();
		try {
			project.getCompileClasspathElements().forEach( element -> {
				FileUtils.iterateFiles( new File( Objects.toString( element ) ), EXTENSIONS, true ).forEachRemaining( file -> {
					classpath.add( file );
				} );
			} );

			getLog().info( "classpath: " + classpath );
		} catch ( DependencyResolutionRequiredException e ) {
			getLog().error( "Exception thrown wihile getting paths: ", e );
		}
		return classpath;
	}

	protected void populatePluginSets( List<File> classpath ) {
		try {
			classpath.forEach( classFile -> {
				try {
					ClassReader classReader = new ClassReader( FileUtils.readFileToByteArray( classFile ) );
					for ( PluginType pluginType : PLUGIN_TYPES ) {
						pluginType.addClass( classReader );
					}
				} catch ( Throwable e ) {
					getLog().error( "Exception thrown wihile getting paths: ", e );
				}
			} );
		} catch ( Throwable e ) {
			getLog().error( "Exception thrown wihile getting paths: ", e );
		}
	}

	protected File makePFInfDir() {
		File dir = null;

		try {
			String targetClassesDir = Objects.toString( project.getCompileClasspathElements().stream().findFirst().orElseThrow() );
			dir = new File( targetClassesDir + IOUtils.DIR_SEPARATOR + PF_INF );

			FileUtils.forceMkdir( dir );
		} catch ( Throwable e ) {
			getLog().error( "Exception thrown wihile getting paths: ", e );
		}

		return dir;
	}

	protected class PluginType {

		final String fileName;
		final String[] classNames;
		final Set<String> classes = new HashSet<>();

		public PluginType( String fileName, String... classNames ) {
			this.fileName = fileName;
			this.classNames = classNames;
		}

		public boolean addClass( ClassReader classReader ) {
			return containsClass( classReader ) ? classes.add( dotted( classReader.getClassName() ) ) : false;
		}

		public boolean containsClass( ClassReader classReader ) {
			for ( String className : classNames ) {
				for ( String iface : classReader.getInterfaces() ) {
					if ( StringUtils.equals( className, dotted( iface ) ) ) {
						return true;
					}
				}
			}

			return false;
		}

		public String dotted( String str ) {
			return StringUtils.replace( str, "/", "." );
		}

	}

}
