import { useNavigate } from 'react-router-dom';
import mediflowIcon from '../assets/mediflow-icon.svg';
import styles from './TermsOfServicePage.module.scss';

export default function TermsOfServicePage() {
    const navigate = useNavigate();

    return (
        <div className={styles.container}>
            <div className={styles.wrapper}>
                {/* 헤더 */}
                <div className={styles.header}>
                    <div className={styles.logoWrapper}>
                        <img src={mediflowIcon} alt="MediFlow" />
                    </div>
                    <h1 className={styles.title}>이용약관</h1>
                    <p className={styles.date}>최종 수정일: 2025년 11월 24일</p>
                </div>

                {/* 내용 */}
                <div className={styles.content}>
                    <section className={styles.section}>
                        <h2>제1조 (목적)</h2>
                        <p>
                            본 약관은 MediFlow(이하 "서비스")가 제공하는 전자의무기록(EMR) 시스템의 이용과 관련하여
                            서비스 제공자와 이용자 간의 권리, 의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.
                        </p>
                    </section>

                    <section className={styles.section}>
                        <h2>제2조 (용어의 정의)</h2>
                        <ul>
                            <li>"서비스"란 MediFlow가 제공하는 전자의무기록 관리 시스템을 의미합니다.</li>
                            <li>"이용자"란 본 약관에 동의하고 서비스를 이용하는 의료기관 종사자(간호사 포함)를 의미합니다.</li>
                            <li>"환자정보"란 서비스를 통해 입력, 조회, 관리되는 모든 환자 관련 개인정보 및 의료정보를 의미합니다.</li>
                            <li>"계정"이란 이용자 식별과 서비스 이용을 위해 이용자가 설정한 이메일 및 비밀번호 조합을 의미합니다.</li>
                        </ul>
                    </section>

                    <section className={styles.section}>
                        <h2>제3조 (서비스의 제공)</h2>
                        <p>
                            서비스는 간호사의 업무 효율성 향상을 위해 다음과 같은 기능을 제공합니다:
                        </p>
                        <ul>
                            <li>환자 의무기록 작성 및 조회</li>
                            <li>투약 기록 관리</li>
                            <li>바이탈 사인 기록 및 모니터링</li>
                            <li>간호 일지 작성</li>
                            <li>의료진 간 정보 공유</li>
                        </ul>
                    </section>

                    <section className={styles.section}>
                        <h2>제4조 (이용자의 의무)</h2>
                        <ul>
                            <li>이용자는 본인의 계정 정보를 안전하게 관리해야 하며, 타인에게 공유하거나 양도할 수 없습니다.</li>
                            <li>이용자는 의료법 및 개인정보보호법 등 관련 법령을 준수해야 합니다.</li>
                            <li>환자정보는 업무상 필요한 범위 내에서만 접근 및 이용해야 합니다.</li>
                            <li>허가되지 않은 목적으로 환자정보를 열람, 복사, 유출하는 행위는 금지됩니다.</li>
                            <li>이용자는 정확한 정보를 입력해야 하며, 허위 정보 입력으로 인한 책임은 이용자에게 있습니다.</li>
                        </ul>
                    </section>

                    <section className={styles.section}>
                        <h2>제5조 (개인정보 보호)</h2>
                        <p>
                            서비스는 이용자 및 환자의 개인정보를 관련 법령에 따라 안전하게 보호합니다.
                            개인정보의 수집, 이용, 제공, 관리 등에 관한 사항은 별도의 개인정보처리방침에 따릅니다.
                        </p>
                    </section>

                    <section className={styles.section}>
                        <h2>제6조 (서비스의 중단)</h2>
                        <p>
                            서비스는 다음의 경우 서비스 제공을 일시적으로 중단할 수 있습니다:
                        </p>
                        <ul>
                            <li>시스템 정기점검 및 유지보수</li>
                            <li>천재지변, 비상사태 등 불가항력적 사유</li>
                            <li>서비스 설비의 장애 또는 서비스 이용 폭주</li>
                        </ul>
                    </section>

                    <section className={styles.section}>
                        <h2>제7조 (면책조항)</h2>
                        <ul>
                            <li>서비스는 이용자가 입력한 정보의 정확성에 대해 책임지지 않습니다.</li>
                            <li>이용자의 귀책사유로 인한 서비스 이용 장애에 대해서는 책임을 지지 않습니다.</li>
                            <li>의료 행위의 최종 책임은 해당 의료기관 및 의료진에게 있습니다.</li>
                        </ul>
                    </section>

                    <section className={styles.section}>
                        <h2>제8조 (약관의 변경)</h2>
                        <p>
                            본 약관은 관련 법령 및 서비스 정책 변경에 따라 수정될 수 있으며,
                            변경된 약관은 서비스 내 공지사항을 통해 공지됩니다.
                        </p>
                    </section>

                    <section className={styles.section}>
                        <h2>제9조 (준거법 및 관할법원)</h2>
                        <p>
                            본 약관과 관련된 분쟁은 대한민국 법률을 준거법으로 하며,
                            관할법원은 민사소송법에 따라 정해집니다.
                        </p>
                    </section>

                    <div className={styles.footer}>
                        <p>
                            <strong>시행일:</strong> 2025년 11월 24일
                        </p>
                    </div>
                </div>

                {/* 버튼 */}
                <div className={styles.buttonGroup}>
                    <button
                        onClick={() => navigate(-1)}
                        className={styles.secondaryButton}
                    >
                        뒤로 가기
                    </button>
                    <button
                        onClick={() => navigate('/')}
                        className={styles.primaryButton}
                    >
                        홈으로
                    </button>
                </div>
            </div>
        </div>
    );
}

