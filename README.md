# parent
探索开源项目，察其精髓


1.新增RPCProxy代理，动态创建代理对象发送请求。

2.新增PRCFuture，请求使用RPCFuture代理，使用requestId做标识。把请求由rpcClient无状态的发送，
改为在RpcClientHandler中根据请求id发送，并且记录在map中，channelRead0()根据id找future。

3.在RPCServer中增加线程池，在handler->channelRead0()方法中，每个请求都提交到线程池中。