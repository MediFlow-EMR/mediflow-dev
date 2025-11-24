import { useNavigate } from 'react-router-dom';
import mediflowIcon from '../assets/mediflow-icon.svg';
import styles from './PrivacyPolicyPage.module.scss';

export default function PrivacyPolicyPage() {
    const navigate = useNavigate();

    return (
        <div className={styles.container}>
            <div className={styles.wrapper}>
                {/* 헤더 */}
                <div className={styles.header}>
                    <div className={styles.logoWrapper}>
                        <img src={mediflowIcon} alt="MediFlow" />
                    </div>
                    <h1 className={styles.title}>개인정보처리방침</h1>
                    <p className={styles.date}>최종 수정일: 2025년 11월 24일</p>
                </div>

                {/* 내용 */}
                <div className={styles.content}>
                    <section className={styles.section}>
                        <p>
                            MediFlow(이하 "서비스")는 「개인정보보호법」, 「의료법」, 「생명윤리 및 안전에 관한 법률」 등
                            관련 법령에 따라 이용자 및 환자의 개인정보를 보호하고, 이와 관련한 고충을 신속하고 원활하게 처리할 수 있도록
                            다음과 같이 개인정보처리방침을 수립·공개합니다.
                        </p>
                    </section>

                    <section className={styles.section}>
                        <h2>제1조 (개인정보의 처리 목적)</h2>
                        <p>
                            서비스는 다음의 목적을 위하여 개인정보를 처리합니다:
                        </p>
                        <div className={styles.subsection}>
                            <div>
                                <p>1. 이용자 정보</p>
                                <ul>
                                    <li>회원 가입 및 관리</li>
                                    <li>본인 확인 및 인증</li>
                                    <li>서비스 제공 및 업무 권한 관리</li>
                                    <li>서비스 부정이용 방지</li>
                                </ul>
                            </div>
                            <div>
                                <p>2. 환자 정보</p>
                                <ul>
                                    <li>의료 서비스 제공 및 진료 기록 관리</li>
                                    <li>환자 식별 및 진료 연속성 보장</li>
                                    <li>의료법에 따른 진료기록 보존</li>
                                </ul>
                            </div>
                        </div>
                    </section>

                    <section className={styles.section}>
                        <h2>제2조 (처리하는 개인정보의 항목)</h2>
                        <div className={styles.subsection}>
                            <div>
                                <p>1. 이용자(의료진)</p>
                                <ul>
                                    <li>필수항목: 이메일, 비밀번호(암호화), 이름, 소속 부서, 직급</li>
                                    <li>자동수집: 서비스 이용기록, 접속 IP, 접속 시각</li>
                                </ul>
                            </div>
                            <div>
                                <p>2. 환자</p>
                                <ul>
                                    <li>필수항목: 이름, 생년월일, 성별, 환자번호</li>
                                    <li>의료정보: 진단명, 투약정보, 바이탈 사인, 간호기록, 검사결과 등</li>
                                </ul>
                            </div>
                        </div>
                    </section>

                    <section className={styles.section}>
                        <h2>제3조 (개인정보의 처리 및 보유 기간)</h2>
                        <div className={styles.subsection}>
                            <p>1. 이용자 정보</p>
                            <ul>
                                <li>회원 탈퇴 시까지 보유 (단, 관련 법령에 따라 일부 정보는 별도 보관)</li>
                            </ul>
                            <p>2. 환자 정보</p>
                            <ul>
                                <li>의료법 제22조에 따라 진료기록은 10년간 보존</li>
                                <li>환자의 동의 없이 임의 ��제 불가</li>
                            </ul>
                        </div>
                    </section>

                    <section className={styles.section}>
                        <h2>제4조 (개인정보의 제3자 제공)</h2>
                        <p>
                            서비스는 원칙적으로 이용자 및 환자의 개인정보를 외부에 제공하지 않습니다.
                            다만, 다음의 경우는 예외로 합니다:
                        </p>
                        <ul>
                            <li>법령에 따라 제공이 요구되는 경우</li>
                            <li>환자 본인 또는 법정대리인의 동의가 있는 경우</li>
                            <li>의료법에 따른 정당한 진료 목적으로 다른 의료기관과 공유하는 경우</li>
                        </ul>
                    </section>

                    <section className={styles.section}>
                        <h2>제5조 (개인정보의 파기)</h2>
                        <div className={styles.subsection}>
                            <p>파기절차</p>
                            <p>
                                이용목적이 달성된 개인정보는 즉시 파기하며,
                                법령에 따라 보존해야 하는 경우 별도 DB로 분리 보관 후 해당 기간 경과 시 파기합니다.
                            </p>
                            <p>파기방법</p>
                            <ul>
                                <li>전자파일: 복구 불가능한 방법으로 영구 삭제</li>
                                <li>종이문서: 분쇄 또는 소각</li>
                            </ul>
                        </div>
                    </section>

                    <section className={styles.section}>
                        <h2>제6조 (개인정보의 안전성 확보 조치)</h2>
                        <p>
                            서비스는 개인정보의 안전성 확보를 위해 다음과 같은 조치를 취하고 있습니다:
                        </p>
                        <ul>
                            <li>개인정보 암호화: 비밀번호, 민감정보는 암호화하여 저장 및 전송</li>
                            <li>해킹 등에 대비한 기술적 대책: 방화벽, 침입탐지시스템 운영</li>
                            <li>접근 권한 관리: 개인정보 접근 권한을 최소한의 인원으로 제한</li>
                            <li>접속기록 보관: 개인정보 접근 기록을 최소 6개월 이상 보관</li>
                            <li>정기적인 보안 점검 및 직원 교육</li>
                        </ul>
                    </section>

                    <section className={styles.section}>
                        <h2>제7조 (정보주체의 권리·의무 및 행사방법)</h2>
                        <p>
                            이용자는 언제든지 다음의 권리를 행사할 수 있습니다:
                        </p>
                        <ul>
                            <li>개인정보 열람 요구</li>
                            <li>개인정보 정정·삭제 요구</li>
                            <li>개인정보 처리정지 요구</li>
                            <li>회원 탈퇴(동의 철회)</li>
                        </ul>
                        <p>
                            권리 행사는 서비스 내 설정 메뉴 또는 개인정보보호 담당자에게 서면, 이메일 등으로 요청할 수 있습니다.
                        </p>
                    </section>

                    <section className={styles.section}>
                        <h2>제8조 (개인정보 자동 수집 장치의 설치·운영 및 거부)</h2>
                        <p>
                            서비스는 이용자의 서비스 이용 기록, 접속 빈도 등을 분석하기 위해 쿠키(Cookie)를 사용할 수 있습니다.
                            이용자는 웹브라우저 설정을 통해 쿠키 저장을 거부할 수 있으나,
                            이 경우 일부 서비스 이용에 제한이 있을 수 있습니다.
                        </p>
                    </section>

                    <section className={styles.section}>
                        <h2>제9조 (개인정보보호 책임자)</h2>
                        <div className={styles.contactBox}>
                            <p>개인정보보호 책임자</p>
                            <ul>
                                <li>• 성명: [담당자 성명]</li>
                                <li>• 직책: [담당자 직책]</li>
                                <li>• 이메일: privacy@mediflow.com</li>
                                <li>• 전화: 000-0000-0000</li>
                            </ul>
                        </div>
                    </section>

                    <section className={styles.section}>
                        <h2>제10조 (권익침해 구제방법)</h2>
                        <p>
                            개인정보 침해로 인한 신고나 상담이 필요한 경우 다음 기관에 문의하실 수 있습니다:
                        </p>
                        <ul>
                            <li>• 개인정보침해신고센터: privacy.kisa.or.kr (국번없이 118)</li>
                            <li>• 대검찰청 사이버수사과: www.spo.go.kr (국번없이 1301)</li>
                            <li>• 경찰청 사이버안전국: cyberbureau.police.go.kr (국번없이 182)</li>
                        </ul>
                    </section>

                    <section className={styles.section}>
                        <h2>제11조 (개인정보처리방침의 변경)</h2>
                        <p>
                            본 개인정보처리방침은 법령, 정책 또는 보안기술의 변경에 따라 내용의 추가, 삭제 및 수정이 있을 시
                            시행일자 최소 7일 전에 서비스 내 공지사항을 통해 고지할 것입니다.
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

