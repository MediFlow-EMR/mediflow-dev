import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaClock, FaCheckCircle, FaUsers, FaShieldAlt } from 'react-icons/fa';
import Header from '../components/landing/Header';
import styles from './LandingPage.module.scss';

const LandingPage = () => {
    const navigate = useNavigate();
    const sectionsRef = useRef([]);

    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add(styles.visible);
                    }
                });
            },
            {
                threshold: 0.1,
                rootMargin: '0px 0px -100px 0px'
            }
        );

        const sections = sectionsRef.current;
        sections.forEach((section) => {
            if (section) observer.observe(section);
        });

        return () => {
            sections.forEach((section) => {
                if (section) observer.unobserve(section);
            });
        };
    }, []);

    const handleCTAClick = () => {
        navigate('/auth/signin');
    };

    return (
        <div className={styles.landingPage}>
            <Header />

            {/* Hero Section */}
            <section 
                className={`${styles.section} ${styles.heroSection}`}
                ref={(el) => (sectionsRef.current[0] = el)}
            >
                <div className={styles.container}>
                    <div className={styles.heroContent}>
                        <h1 className={styles.heroTitle}>
                            간호사의 실무 경험을 기반으로 설계된
                            <br />
                            <span className={styles.highlight}>병동 간호·인수인계 지원 시스템</span>
                        </h1>
                        <p className={styles.heroSubtitle}>
                            환자 정보와 간호 기록을 한곳에서 확인하고
                            <br />
                            AI 인수인계 초안을 검토·저장하는 병동 간호 업무 데모
                        </p>
                        <button 
                            className={styles.ctaButton}
                            onClick={handleCTAClick}
                        >
                            지금 시작하기
                        </button>
                    </div>
                    <div className={styles.heroVisual}>
                        <div className={styles.demoContainer}>
                            <img 
                                src="/assets/aihandover.gif"
                                alt="MediFlow AI 인수인계 자동 요약 데모" 
                                className={styles.demoImage}
                            />
                        </div>
                    </div>
                </div>
            </section>

            {/* Features Section */}
            <section 
                className={`${styles.section} ${styles.featuresSection}`}
                ref={(el) => (sectionsRef.current[1] = el)}
            >
                <div className={styles.container}>
                    <h2 className={styles.sectionTitle}>핵심 기능</h2>
                    <p className={styles.sectionSubtitle}>
                        환자 확인, 간호 수행 기록, 근무조 인수인계를 연결하는 실무 중심 설계
                    </p>
                    
                    <div className={styles.featuresGrid}>
                        <div className={styles.featureCard}>
                            <div className={styles.featurePreview}>
                                <img 
                                    src="/assets/patientdashboard.gif"
                                    alt="통합 환자 뷰" 
                                    className={styles.featureImage}
                                />
                            </div>
                            <div>
                                <h3 className={styles.featureTitle}>통합 환자 뷰 (One-View Dashboard)</h3>
                                <p className={styles.featureDescription}>
                                    담당·부서 환자를 선택해 최근 V/S와 알레르기를 확인하고,
                                    오더, 간호 기록, 섭취·배설량, 투약, 검사 결과를
                                    같은 화면의 탭에서 조회·기록합니다.
                                </p>
                            </div>
                        </div>

                        <div className={styles.featureCard}>
                            <div className={styles.featurePreview}>
                                <img 
                                    src="/assets/nursingrecord.gif"
                                    alt="AI 인수인계" 
                                    className={styles.featureImage}
                                />
                            </div>
                            <div>
                                <h3 className={styles.featureTitle}>AI 인수인계 초안</h3>
                                <p className={styles.featureDescription}>
                                    근무조의 간호 기록과 수행 정보를 바탕으로 초안을 생성합니다.
                                    환자별로 다시 생성할 수 있으며, 사용자가 내용을 검토·수정한 뒤
                                    인수인계로 저장합니다.
                                </p>
                            </div>
                        </div>

                        <div className={styles.featureCard}>
                            <div className={styles.featurePreview}>
                                <img 
                                    src="/assets/ordertab.gif"
                                    alt="의료 오더" 
                                    className={styles.featureImage}
                                />
                            </div>
                            <div>
                                <h3 className={styles.featureTitle}>오더 관리와 약품 정보</h3>
                                <p className={styles.featureDescription}>
                                    데모 처방의 수행 상태를 관리하고, 식약처 의약품 API로
                                    제품명·함량·효능효과·용법용량·주의사항을 확인합니다.
                                    실제 병원 처방 시스템과의 연계는 구현 범위 밖입니다.
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className={styles.ctaContainer}>
                        <button 
                            className={styles.ctaButton}
                            onClick={handleCTAClick}
                        >
                            지금 시작하기
                        </button>
                    </div>
                </div>
            </section>

            {/* Value Proposition Section */}
            <section 
                className={`${styles.section} ${styles.valueSection}`}
                ref={(el) => (sectionsRef.current[2] = el)}
            >
                <div className={styles.container}>
                    <h2 className={styles.sectionTitle}>왜 MediFlow인가?</h2>
                    <p className={styles.sectionSubtitle}>
                        간호사의 실무 경험에서 출발한 현장 중심 솔루션
                    </p>

                    <div className={styles.valueGrid}>
                        <div className={styles.valueItem}>
                            <div className={styles.valueIcon}>
                                <FaClock />
                            </div>
                            <h3 className={styles.valueTitle}>한곳에서 환자 확인</h3>
                            <p className={styles.valueDescription}>
                                환자 목록과 상세 정보를 나란히 배치해 환자를 바꿔가며 확인할 수 있습니다.
                                간호 업무별 기록은 상세 화면의 탭으로 이동합니다.
                            </p>
                        </div>

                        <div className={styles.valueItem}>
                            <div className={styles.valueIcon}>
                                <FaCheckCircle />
                            </div>
                            <h3 className={styles.valueTitle}>인수인계 검토 지원</h3>
                            <p className={styles.valueDescription}>
                                기록을 바탕으로 생성한 초안을 확인하며 인수인계를 작성합니다.
                                AI 결과에는 오류나 누락이 있을 수 있어 사용자의 검토가 필요합니다.
                            </p>
                        </div>

                        <div className={styles.valueItem}>
                            <div className={styles.valueIcon}>
                                <FaUsers />
                            </div>
                            <h3 className={styles.valueTitle}>간호 업무 흐름 체험</h3>
                            <p className={styles.valueDescription}>
                                직관적인 UI/UX로 정보 위치를 쉽게 파악할 수 있어 
                                간호 기록과 오더의 흐름을 가상 환자로 체험할 수 있습니다.
                                실제 업무 성과나 교육 효과를 검증한 제품은 아닙니다.
                            </p>
                        </div>

                        <div className={styles.valueItem}>
                            <div className={styles.valueIcon}>
                                <FaShieldAlt />
                            </div>
                            <h3 className={styles.valueTitle}>AWS 기반 배포</h3>
                            <p className={styles.valueDescription}>
                                S3·CloudFront로 웹 화면을 제공하고 EC2·RDS로 서버와 데이터를 운영합니다.
                                Lambda는 약품 정보 캐시를 생성하며, GitHub Actions로 웹·서버를 배포합니다.
                            </p>
                        </div>
                    </div>

                    <div className={styles.ctaContainer}>
                        <button 
                            className={styles.ctaButton}
                            onClick={handleCTAClick}
                        >
                            지금 시작하기
                        </button>
                    </div>
                </div>
            </section>

            {/* Footer */}
            <footer className={styles.footer}>
                <div className={styles.container}>
                    <div className={styles.footerContent}>
                        <div className={styles.footerLogo}>
                            <span className={styles.logoText}>MediFlow</span>
                            <p className={styles.footerTagline}>
                                가상 환자로 체험하는 병동 간호 업무
                            </p>
                        </div>
                        <div className={styles.footerLinks}>
                            <a href="/terms" className={styles.footerLink}>이용약관</a>
                            <a href="/privacy" className={styles.footerLink}>개인정보처리방침</a>
                        </div>
                    </div>
                    <div className={styles.footerBottom}>
                        <p>포트폴리오용 PoC · 모든 환자 데이터는 가상입니다. 실제 의료정보를 입력하지 마세요.</p>
                        <p>&copy; {new Date().getFullYear()} MediFlow. All rights reserved.</p>
                    </div>
                </div>
            </footer>
        </div>
    );
};

export default LandingPage;
