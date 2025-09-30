document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            console.log("Form submitted");

            const formData = {
                username: document.getElementById('username').value,
                password: document.getElementById('password').value
            };

            try {
                console.log("Sending login request");
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(formData)
                });

                console.log("Response received:", response.status);

                const data = await response.json();
                console.log("Data received:", data);

                if (response.ok) {
                    alert('로그인이 완료되었습니다.');
                    if (data.firstLogin) {
                        window.location.href = '/first-login-form'; // 첫 로그인 시 폼으로 이동
                    } else {
                        window.location.href = '/'; // 아닐 경우 홈으로 이동
                    }

                } else {
                    alert(data.message || '로그인에 실패했습니다.');
                }
            } catch (error) {
                console.error("Login error:", error);
                alert('로그인 중 오류가 발생했습니다.');
            }
        });
    }

    if (registerForm) {
        registerForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            const formData = {
                email: document.getElementById('email').value,
                username: document.getElementById('username').value,
                name: document.getElementById('name').value,
                password: document.getElementById('password').value
            };

            try {
                const response = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(formData)
                });

                if (response.ok) {
                    alert('회원가입이 완료되었습니다.');
                    window.location.href = '/login';
                } else {
                    const data = await response.json();
                    alert(data.message || '회원가입에 실패했습니다.');
                }
            } catch (error) {
                alert('회원가입 중 오류가 발생했습니다.');
            }
        });
    }
});