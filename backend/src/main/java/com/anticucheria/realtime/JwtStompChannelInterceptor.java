package com.anticucheria.realtime;

import com.anticucheria.security.CustomUserDetailsService;
import com.anticucheria.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String auth = accessor.getFirstNativeHeader("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        }
        if (token == null || token.isBlank()) {
            Object desdeHandshake = accessor.getSessionAttributes() == null
                    ? null
                    : accessor.getSessionAttributes().get("token");
            if (desdeHandshake instanceof String s) {
                token = s;
            }
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("WebSocket sin JWT");
        }

        String username = jwtUtil.extractUsername(token);
        UserDetails user = userDetailsService.loadUserByUsername(username);
        if (!jwtUtil.isTokenValid(token, user)) {
            throw new IllegalArgumentException("JWT invalido para WebSocket");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return message;
    }
}
