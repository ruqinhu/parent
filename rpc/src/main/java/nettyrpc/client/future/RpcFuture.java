package nettyrpc.client.future;

import nettyrpc.protocol.RpcRequest;
import nettyrpc.protocol.RpcResponse;

import java.util.concurrent.*;

/**
 * @author Zhaorunze
 */
public class RpcFuture implements Future<Object> {

    private RpcRequest rpcRequest;
    private RpcResponse rpcResponse;

    private CountDownLatch countDownLatch;

    private long startTime;

    public RpcFuture(RpcRequest request) {
        countDownLatch = new CountDownLatch(1);
        this.rpcRequest = request;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return rpcResponse != null;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        countDownLatch.await();
        return rpcResponse.getResult();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException {
        Boolean awaitSuccess;
        awaitSuccess = countDownLatch.await(timeout,TimeUnit.MILLISECONDS);
        if (!awaitSuccess) {
            throw new RuntimeException("rpc超时");
        }
        long useTime = System.currentTimeMillis() - startTime;
        System.out.println("has done requestId: " +rpcRequest.getRequestId()+ " class: "+rpcRequest.getClassName() + " method: " + rpcRequest.getMethodName()+" useTime: " + useTime );
        return rpcResponse.getResult();
    }

    /**
     * 在 rpcClientHandler->channelRead0()中被调用，用来把收到的结果和future对应上
     * @param res
     */
    public void done(RpcResponse res) {
        this.rpcResponse = res;
        countDownLatch.countDown();
    }
}
