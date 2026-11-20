package com.example.jobexecutor;

import java.io.PrintStream;

/**
 * 代理 System.out：原样输出的同时，把当前任务线程产生的行写入 JobLogCollector。
 */
public class RoutingPrintStream extends PrintStream {
    private final PrintStream delegate;
    private final JobLogCollector collector;

    public RoutingPrintStream(PrintStream delegate, JobLogCollector collector) {
        super(delegate);
        this.delegate = delegate;
        this.collector = collector;
    }

    @Override
    public void println(String x) {
        synchronized (this) {
            delegate.println(x);
            collector.append(x);
        }
    }

    @Override
    public void println(Object x) {
        println(String.valueOf(x));
    }

    @Override
    public void print(String s) {
        delegate.print(s);
    }

    @Override
    public void print(Object obj) {
        print(String.valueOf(obj));
    }
}
