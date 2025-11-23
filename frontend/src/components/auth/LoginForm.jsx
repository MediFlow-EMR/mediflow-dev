import { Form, Link } from 'react-router-dom';

const LoginForm = () => (
    <>
        <Form method='post' className="space-y-6">
            <h2 className="text-2xl font-bold text-gray-800 text-center mb-6">
                이메일로 로그인
            </h2>

            <div>
                <label
                    htmlFor='email'
                    className="block text-sm font-medium text-gray-700 mb-2"
                >
                    이메일
                </label>
                <input
                    id='email'
                    type='email'
                    name='email'
                    required
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                    placeholder="example@email.com"
                />
            </div>

            <div>
                <label
                    htmlFor='password'
                    className="block text-sm font-medium text-gray-700 mb-2"
                >
                    비밀번호
                </label>
                <input
                    id='password'
                    type='password'
                    name='password'
                    required
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                    placeholder="비밀번호를 입력하세요"
                />
            </div>

            <button
                type='submit'
                className="w-full py-3 px-4 bg-sky-500 hover:bg-sky-600 text-white rounded-lg font-medium transition-colors duration-200 shadow-md"
            >
                로그인
            </button>

            <div className="text-center pt-4 border-t border-gray-200">
                <Link
                    to='/sign-up'
                    className="text-sky-600 hover:text-sky-700 font-medium text-sm"
                >
                    회원이 아니십니까? 회원가입을 해보세요
                </Link>
            </div>

            <div className="text-center">
                <Link
                    to='/'
                    className="text-gray-500 hover:text-gray-700 text-sm"
                >
                    ← 뒤로 가기
                </Link>
            </div>
        </Form>
    </>
);

export default LoginForm;