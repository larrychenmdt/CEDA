package com.adaptiveMQ.benchmark;

import com.adaptiveMQ.message.Destination;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.utils.BufferByte;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class MessageCodecBenchmark
{
    static final Destination destination = new Destination("TEST.PUBLISH");

    @State(Scope.Benchmark)
    public static class CodecState
    {
        final BufferByte encodeBodyBuffer = new BufferByte();
        final BufferByte encodeMessageBuffer = new BufferByte();
        final Message encodeMessage = new Message();
        byte[] encodeOutputBytes;

        final BufferByte decodeBuffer = new BufferByte();
        final byte[] decodeInputBytes = MessageCodecBenchmark.encode(encodeMessage, encodeMessageBuffer, encodeBodyBuffer);
    }

    private static void perfTestEncode(final int runNumber)
    {
        final int reps = 10 * 1000 * 1000;
        final CodecState state = new CodecState();
        final MessageCodecBenchmark benchmark = new MessageCodecBenchmark();

        final long start = System.nanoTime();
        for (int i = 0; i < reps; i++)
        {
            benchmark.testEncode(state);
        }

        final long totalDuration = System.nanoTime() - start;

        System.out.printf(
                "%d - %d(ns) average duration for %s.testEncode() - message encodedLength %d%n",
                runNumber,
                totalDuration / reps,
                benchmark.getClass().getName(),
                state.encodeOutputBytes.length);
    }

    private static void perfTestDecode(final int runNumber)
    {
        final int reps = 10 * 1000 * 1000;
        final CodecState state = new CodecState();
        final MessageCodecBenchmark benchmark = new MessageCodecBenchmark();

        final long start = System.nanoTime();
        for (int i = 0; i < reps; i++)
        {
            benchmark.testDecode(state);
        }

        final long totalDuration = System.nanoTime() - start;

        System.out.printf(
                "%d - %d(ns) average duration for %s.testDecode() - message encodedLength %d%n",
                runNumber,
                totalDuration / reps,
                benchmark.getClass().getName(),
                state.decodeInputBytes.length);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public static int testEncode(final CodecState state)
    {
        state.encodeOutputBytes = encode(state.encodeMessage, state.encodeMessageBuffer, state.encodeBodyBuffer);
        return state.encodeOutputBytes.length;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public static int testDecode(final CodecState state)
    {
        MessageConverter.StrByte2MsgOutput strByte2MsgOutput = decode(state.decodeBuffer, state.decodeInputBytes, state.decodeInputBytes.length);
        return strByte2MsgOutput.iVaildByte;
    }

    private static byte[] encode(Message msg, BufferByte msgBuf, BufferByte bodyBuf)
    {
        try {
            msg.setDestination(destination);
            MessageBody body = msg.getMessageBody();
            body.addInt((short) 5, 0);
            body.addInt((short) 7, 123456789);
            body.addLong((short) 9, 123456789123456789l);
            body.addShort((short) 11, (short) 1234);
            body.addString((short) 3, "hello ceda");
            byte[] bstream = MessageConverter.msg2Byte(msgBuf, bodyBuf, msg);
            byte[] bstr = MessageConverter.getWsStream(bstream, msgBuf);
            return bstr;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private static MessageConverter.StrByte2MsgOutput decode(BufferByte decodeBuffer, byte[] btInput, int iInputLen)
    {
        try {
            MessageConverter.StrByte2MsgOutput strByte2MsgOutput =
                    MessageConverter.byte2VaildMsg(decodeBuffer, btInput, iInputLen);
            return strByte2MsgOutput;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static void main(final String[] args)
    {
        for (int i = 0; i < 10; i++)
        {
            perfTestEncode(i);
            perfTestDecode(i);
        }
    }
}
