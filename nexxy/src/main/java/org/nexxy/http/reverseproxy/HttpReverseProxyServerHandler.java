/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.nexxy.http.reverseproxy;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.ning.http.client.Response;


public class HttpReverseProxyServerHandler extends ChannelHandlerAdapter {
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
    
    private void executeGetAndPopulateCache(String url) throws InterruptedException, ExecutionException, IOException { 
    	Integer test = Cache.getAsInt("status:" + url);
	    if (test == null) {	
		    Future<Response> f = Cache.getClient().prepareGet(url).execute();
		    Response r = f.get();
		    
		    Cache.set("body:" + url, r.getResponseBodyAsBytes());
		    Cache.set("status:" + url, new Integer(r.getStatusCode()));
		    Cache.set("statusText:" + url, r.getStatusText());
		    Cache.set("type:" + url, r.getContentType());
	    	System.out.println(" ***** " + url + " saved in cache\n\r");
	    	System.out.println(" ***** cache contains " + Cache.size() + " elements\n\r");
	    }
    }
    
    private Response executeGet(String url) throws InterruptedException, ExecutionException, IOException { 
	    Future<Response> f = Cache.getClient().prepareGet(url).execute();
	    return f.get();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException, ExecutionException, IOException {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            if (HttpHeaderUtil.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            boolean keepAlive = HttpHeaderUtil.isKeepAlive(req);
            
            String hostName = req.headers().get(HttpHeaders.Names.HOST);
            hostName = hostName.substring(0, hostName.indexOf(":"));
            Route route = Route.findBySource(hostName);
        	final String url = "http://" + route.destination + req.uri();
        	FullHttpResponse response = null;
        	
            if (route.cache > 0) {
				executeGetAndPopulateCache(url);
	            response = new DefaultFullHttpResponse(
	            		HTTP_1_1, OK, 
	            		Unpooled.wrappedBuffer(Cache.getAsByteArray("body:" + url))
	            );
	            response.setStatus(new HttpResponseStatus(Cache.getAsInt("status:" + url), Cache.getAsString("statusText:" + url)));
	            response.headers().set(CONTENT_TYPE, Cache.getAsString("type:" + url));
            } else {
            	final Response upstreamResponse = executeGet(url);
        	    response = new DefaultFullHttpResponse(
                		HTTP_1_1, OK, 
                		Unpooled.wrappedBuffer(upstreamResponse.getResponseBodyAsBytes())
                );
	            response.setStatus(new HttpResponseStatus(upstreamResponse.getStatusCode(), upstreamResponse.getStatusText()));
	            response.headers().set(CONTENT_TYPE, upstreamResponse.getContentType());
            }

            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

            System.out.println(" ***** " + url + " served");
            
            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, Values.KEEP_ALIVE);
                ctx.write(response);
            }

        }
        
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
