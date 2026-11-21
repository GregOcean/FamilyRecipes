package com.familyrecipes.monitoring;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.Charset;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RequestMetricsFilter extends OncePerRequestFilter {
    private final RequestMetrics metrics;
    public RequestMetricsFilter(RequestMetrics metrics) { this.metrics=metrics; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/admin/");
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        long start=System.nanoTime(); metrics.started(); var output=new CountingResponse(res); boolean failed=false;
        try { chain.doFilter(req,output); }
        catch(ServletException|IOException|RuntimeException e) { failed=true; throw e; }
        finally {
            if(output.writer!=null)output.writer.flush();
            Object route=req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            metrics.finished(req.getMethod()+" "+(route==null?"(unmatched)":route.toString()),failed?500:res.getStatus(),
                (System.nanoTime()-start)/1_000_000.0,req.getContentLengthLong(),output.count);
        }
    }
    private static class CountingResponse extends HttpServletResponseWrapper {
        long count; ServletOutputStream stream; PrintWriter writer;
        CountingResponse(HttpServletResponse response){super(response);}
        @Override public ServletOutputStream getOutputStream() throws IOException {
            if(stream==null) {var original=super.getOutputStream();stream=new ServletOutputStream(){
                public boolean isReady(){return original.isReady();} public void setWriteListener(WriteListener l){original.setWriteListener(l);}
                public void write(int b)throws IOException{original.write(b);count++;}
                public void write(byte[] b,int off,int len)throws IOException{original.write(b,off,len);count+=len;}
                public void flush()throws IOException{original.flush();}
            };} return stream;
        }
        @Override public PrintWriter getWriter() throws IOException {
            if(writer==null)writer=new PrintWriter(new OutputStreamWriter(getOutputStream(),Charset.forName(getCharacterEncoding())),true);
            return writer;
        }
        @Override public void flushBuffer()throws IOException{if(writer!=null)writer.flush();super.flushBuffer();}
    }
}
