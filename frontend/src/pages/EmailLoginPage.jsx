import LoginForm from '../components/auth/LoginForm';
import mediflowIcon from '../assets/mediflow-icon.svg';

export default function EmailLoginPage() {
    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-sky-50 flex items-center justify-center px-4">
            <div className="w-full max-w-md">
                {/* 로고 섹션 */}
                <div className="text-center mb-8">
                    <div className="flex justify-center mb-4">
                        <img src={mediflowIcon} alt="MediFlow" className="w-20 h-20" />
                    </div>
                    <h1 className="text-3xl font-bold text-sky-700 mb-2">
                        MediFlow
                    </h1>
                    <p className="text-sky-600 text-sm">전자의무기록 시스템</p>
                </div>

                {/* 로그인 폼 */}
                <div className="bg-white rounded-2xl shadow-lg p-8">
                    <LoginForm />
                </div>
            </div>
        </div>
    );
}

