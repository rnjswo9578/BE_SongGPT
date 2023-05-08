package com.team2.songgpt.service;

import com.team2.songgpt.dto.member.*;
import com.team2.songgpt.entity.Member;
import com.team2.songgpt.entity.RefreshToken;
import com.team2.songgpt.global.dto.ResponseDto;
import com.team2.songgpt.global.jwt.JwtUtil;
import com.team2.songgpt.repository.MemberRepository;
import com.team2.songgpt.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public ResponseDto<MemberResponseDto> getMember(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request, JwtUtil.ACCESS_TOKEN);

        tokenNullCheck(token);
        tokenValidateCheck(token);

        String userInfo = jwtUtil.getUserInfoFromToken(token);
        Member member = memberRepository.findByEmail(userInfo).orElseThrow(
                () -> new IllegalArgumentException("토큰이 유효하지 않습니다.")
        );
        MemberResponseDto memberResponseDto = new MemberResponseDto(member);
        return ResponseDto.setSuccess("Success", memberResponseDto);
    }

    /**
     * 회원가입
     */
    @Transactional
    public ResponseDto<?> signup(@RequestBody SignupRequestDto signupRequestDto) {
        String email = signupRequestDto.getEmail();
        String password = signupRequestDto.getPassword();
        String nickname = signupRequestDto.getNickname();

        password = passwordEncoder.encode(password);

        Optional<Member> findEmail = memberRepository.findByEmail(email);
        if (findEmail.isPresent()) {
            throw new IllegalArgumentException("이미 등록된 회원입니다.");
        }

        Optional<Member> findNickname = memberRepository.findByNickname(nickname);
        if (findNickname.isPresent()) {
            throw new IllegalArgumentException("이미 등록된 회원입니다.");
        }

        Member member = new Member(email, password, nickname);
        memberRepository.save(member);
        return ResponseDto.setSuccess("Success", null);
    }

    /**
     * 로그인
     */
    @Transactional
    public ResponseDto<LoginResponseDto> login(LoginRequestDto loginRequestDto, HttpServletResponse response) {
        String email = loginRequestDto.getEmail();
        String password = loginRequestDto.getPassword();

        Member member = memberRepository.findByEmail(email).orElseThrow(
                () -> new IllegalArgumentException("등록되지 않은 회원입니다.")
        );

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        TokenDto tokenDto = jwtUtil.createAllToken(member.getEmail());
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByEmail(member.getEmail());

        if (refreshToken.isPresent()) {
            refreshTokenRepository.save(refreshToken.get().updateToken(tokenDto.getRefreshToken()));
        } else {
            RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), email);
            refreshTokenRepository.save(newToken);
        }

        setHeader(response, tokenDto);
        LoginResponseDto loginResponseDto = new LoginResponseDto(member);
        return ResponseDto.setSuccess("Success", loginResponseDto);
    }

    /**
     * 로그아웃
     */
    @Transactional
    public ResponseDto<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtUtil.resolveToken(request, JwtUtil.ACCESS_TOKEN);

        tokenNullCheck(token);
        tokenValidateCheck(token);

        String userInfo = jwtUtil.getUserInfoFromToken(token);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && userInfo.equals(authentication.getName())) {
            //만료시간이 현재 시간 이전으로 설정된 accessToken을 만들어서 클라이언트에 보냄
            Date now = new Date();
            Date expiredDate = new Date(now.getTime() - 1000);
            String newToken = jwtUtil.createExpiredToken(userInfo, JwtUtil.ACCESS_TOKEN, expiredDate);
            SecurityContextHolder.getContext().setAuthentication(null);
            response.setHeader("Access_Token", newToken);
            return ResponseDto.setSuccess("Success", null);
        }
        throw new IllegalArgumentException("인증이 유효하지 않습니다.");
    }

    @Transactional
    public ResponseDto<?> callNewAccessToken(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        String token = jwtUtil.resolveToken(request, JwtUtil.ACCESS_TOKEN);
        tokenNullCheck(token);

        boolean isRefreshToken = jwtUtil.refreshTokenValidation(refreshToken);
        if (!isRefreshToken) {
            throw new IllegalArgumentException("토큰이 유효하지 않습니다.");
        }

        String email = jwtUtil.getUserInfoFromToken(refreshToken);
        String newAccessToken = jwtUtil.createToken(email, JwtUtil.ACCESS_TOKEN);
        jwtUtil.setHeaderAccessToken(response, newAccessToken);

        String newRefreshToken = jwtUtil.createToken(email, JwtUtil.REFRESH_TOKEN);
        newRefreshToken = newRefreshToken.substring(7);

        Cookie cookie = new Cookie(JwtUtil.REFRESH_TOKEN, newRefreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        response.setHeader("Access_Token", newAccessToken);
        return ResponseDto.setSuccess("Success", null);
    }

    //응답 헤더에 액세스, 리프레시 토큰 추가
    public void setHeader(HttpServletResponse response, TokenDto tokenDto) {
        response.addHeader(JwtUtil.ACCESS_TOKEN, tokenDto.getAccessToken());
        response.addHeader(JwtUtil.REFRESH_TOKEN, tokenDto.getRefreshToken());
    }

    private void tokenNullCheck(String token) {
        if (token == null) {
            throw new NullPointerException("토큰이 없습니다.");
        }
    }

    private void tokenValidateCheck(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("토큰이 유효하지 않습니다.");
        }
    }
}