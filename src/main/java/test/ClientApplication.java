package test;

import com.example.demo.repository.domain.Person;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.metadata.CompositeMetadataCodec;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;

import java.net.URI;
import java.util.Collections;
import java.util.logging.Logger;

public class ClientApplication {
    private static final Logger logger = Logger.getLogger(ClientApplication.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {

        logger.info("TESTTTTTTTTTTTTTTTTTTTTTTTT");

        WebsocketClientTransport transport = WebsocketClientTransport.create(URI.create("ws://localhost:7000/rsocket"));
        RSocket rsocketClient = RSocketConnector.create()
                .dataMimeType(WellKnownMimeType.APPLICATION_JSON.getString())
                //metadata header needs to be specified
                .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString())
                // value of spring.rsocket.server.port eg 7000
                .connect(transport)
                .block();

        ByteBuf data = ByteBufAllocator.DEFAULT.buffer().writeBytes("request msg".getBytes());

        CompositeByteBuf metadata = ByteBufAllocator.DEFAULT.compositeBuffer();
        RoutingMetadata routingMetadata = TaggingMetadataCodec.createRoutingMetadata(ByteBufAllocator.DEFAULT, Collections.singletonList("stream"));
        CompositeMetadataCodec.encodeAndAddMetadata(metadata,
                ByteBufAllocator.DEFAULT,
                WellKnownMimeType.MESSAGE_RSOCKET_ROUTING,
                routingMetadata.getContent());

        try {
            rsocketClient.requestStream(DefaultPayload.create(data, metadata))
                    .doOnEach(f -> {
                        Person person = mapPerson(f.get().getDataUtf8());
                        System.out.println(person);
                    })
                    .blockLast();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rsocketClient.dispose();
        }
    }

    private static Person mapPerson(String data) {
        try {
            return objectMapper.readValue(data, Person.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
