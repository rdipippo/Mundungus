package org.deadsimple.mundungus;

import com.mongodb.ServerAddress;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.distribution.GenericVersion;
import de.flapdoodle.embed.process.runtime.Network;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class EmbeddedMongo {
    static MongodStarter starter = null;
    
    final static int port = ServerAddress.defaultPort();
    
    static MongodExecutable mongodExecutable = null;
    
    final static IFeatureAwareVersion mongoVersion = Versions.withFeatures(new GenericVersion("2.6.5"));
    
    public static void start() {
        try {
            Logger logger = Logger.getLogger("org.deadsimple.mundungus.EmbeddedMongo");

            IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                    .defaultsWithLogger(Command.MongoD, logger)
                    .processOutput(ProcessOutput.getDefaultInstanceSilent())
                    .build();

            starter = MongodStarter.getInstance(runtimeConfig);

            final IMongodConfig mongodConfig = new MongodConfigBuilder()
               .version(mongoVersion)
               .net(new Net(port, Network.localhostIsIPv6()))
               .build();
            
            mongodExecutable = starter.prepare(mongodConfig);
            mongodExecutable.start();
        } catch (final UnknownHostException e) {
            throw new RuntimeException("Failed to start embedded MongoDB instance using FlapDoodle. Threw an UnknownHostException, message was " + e.getMessage());
        } catch (final IOException e) {
            throw new RuntimeException("Failed to start embedded MongoDB instance using FlapDoodle. Threw an IOException, message was " + e.getMessage());
        }
    }
    
    public static void stop() {
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
    }
}
