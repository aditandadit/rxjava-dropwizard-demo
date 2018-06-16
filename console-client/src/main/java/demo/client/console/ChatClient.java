package demo.client.console;

import com.google.protobuf.Empty;
import demo.proto.ChatProto;
import demo.proto.RxChatGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import jline.console.ConsoleReader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static demo.client.console.ConsoleUtil.*;

/**
 * Demonstrates building a gRPC streaming client using RxJava and Reactive-Grpc.
 */
public final class ChatClient {
    private static final int PORT = 9999;

    private ChatClient() { }

    public static void main(String[] args) throws Exception {
        // Connect to the sever
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", PORT).usePlaintext().build();
        RxChatGrpc.RxChatStub stub = RxChatGrpc.newRxStub(channel);

        CountDownLatch done = new CountDownLatch(1);
        ConsoleReader console = new ConsoleReader();

        // Prompt the user for their name
        console.println("Press ctrl+D to quit");
        String author = console.readLine("Who are you? > ");
        toMessage(author, author + " joined.").compose(stub::postMessage).subscribe();

        // Subscribe to incoming messages
        Disposable chatSubscription = Single.just(Empty.getDefaultInstance()).as(stub::getMessages).subscribe(
            message -> {
                // Don't re-print our own messages
                if (!message.getAuthor().equals(author)) {
                    printLine(console, message.getAuthor(), message.getMessage());
                }
            },
            throwable -> {
                printLine(console, "ERROR", throwable.getMessage());
                done.countDown();
            },
            done::countDown
        );

        // Publish outgoing messages
        Observable.fromIterable(new ConsoleIterator(console, author + " > "))
            .map(msg -> toMessage(author, msg))
            .flatMapSingle(stub::postMessage)
            .subscribe(
                empty -> { },
                throwable -> {
                    printLine(console, "ERROR", throwable.getMessage());
                    done.countDown();
                },
                done::countDown
            );

        // Wait for a signal to exit, then clean up
        done.await();
        toMessage(author, author + " left.").compose(stub::postMessage).subscribe();
        chatSubscription.dispose();
        channel.shutdown();
        channel.awaitTermination(1, TimeUnit.SECONDS);
        console.getTerminal().restore();
    }

    private static Single<ChatProto.ChatMessage> toMessage(String author, String message) {
        return Single.just(
            ChatProto.ChatMessage.newBuilder()
                .setAuthor(author)
                .setMessage(message)
                .build()
        );
    }
}
