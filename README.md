# Ping Federate Plugin Plugin
Maven Plugin to create a Ping Federate plugin Jar


## Usage

Add the following plugin into your Maven `pom.xml` file:
```
	<build>
		<plugins>
			<plugin>
				<groupId>io.rzem.maven.plugin.ping</groupId>
				<artifactId>ping-federate-jar-maven-plugin</artifactId>
				<version>1.0.0</version>
				<executions>
					<execution>
						<goals>
							<goal>ping-federate-jar</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
```
