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

        sectionsRef.current.forEach((section) => {
            if (section) observer.observe(section);
        });

        return () => {
            sectionsRef.current.forEach((section) => {
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
                            <span className={styles.highlight}>차세대 전자의무기록 시스템</span>
                        </h1>
                        <p className={styles.heroSubtitle}>
                            환자 정보 통합 대시보드와 AI 기반 인수인계 자동화로
                            <br />
                            간호사의 업무 효율을 극대화하는 EMR 시스템
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
                        간호사의 업무 효율을 극대화하는 실무 중심 설계
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
                                    환자 한 명의 모든 정보를 한 화면에서 확인. 
                                    바이탈 사인, I/O, 검사 결과, 투약 기록, 간호 기록까지 
                                    5개 이상의 페이지 이동 없이 1번의 클릭으로 즉시 접근
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
                                <h3 className={styles.featureTitle}>AI 인수인계 자동 요약</h3>
                                <p className={styles.featureDescription}>
                                    Google Gemini AI가 당일 간호 기록을 자동으로 분석하여 
                                    핵심 내용만 요약. 수기 작성 시간을 절감하고 
                                    정보 누락 위험을 최소화
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
                                <h3 className={styles.featureTitle}>실시간 의료 오더 & 약품 정보</h3>
                                <p className={styles.featureDescription}>
                                    의사 처방 오더를 실시간으로 조회하고, 
                                    식약처 API를 통해 약품 정보를 즉시 확인. 
                                    안전한 투약 관리 지원
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
                            <h3 className={styles.valueTitle}>시간 절약</h3>
                            <p className={styles.valueDescription}>
                                기존 EMR은 환자 한 명의 상태 파악을 위해 최소 5개 이상의 페이지를 이동해야 했습니다. 
                                MediFlow는 One-View 대시보드로 1번의 클릭만으로 모든 정보에 접근 가능합니다.
                            </p>
                        </div>

                        <div className={styles.valueItem}>
                            <div className={styles.valueIcon}>
                                <FaCheckCircle />
                            </div>
                            <h3 className={styles.valueTitle}>정보 누락 방지</h3>
                            <p className={styles.valueDescription}>
                                수기 인수인계는 정보 누락 위험이 높습니다. 
                                AI 자동 요약으로 당일 간호 기록의 핵심 내용을 빠짐없이 전달하여 
                                환자 안전을 보장합니다.
                            </p>
                        </div>

                        <div className={styles.valueItem}>
                            <div className={styles.valueIcon}>
                                <FaUsers />
                            </div>
                            <h3 className={styles.valueTitle}>신규 간호사 적응 지원</h3>
                            <p className={styles.valueDescription}>
                                직관적인 UI/UX로 정보 위치를 쉽게 파악할 수 있어 
                                신규 간호사도 빠르게 시스템에 적응하고 
                                환자 상태를 정확히 파악할 수 있습니다.
                            </p>
                        </div>

                        <div className={styles.valueItem}>
                            <div className={styles.valueIcon}>
                                <FaShieldAlt />
                            </div>
                            <h3 className={styles.valueTitle}>AWS 기반 안정성</h3>
                            <p className={styles.valueDescription}>
                                AWS 3-Tier 아키텍처로 구축되어 높은 가용성과 확장성을 보장합니다. 
                                RDS 자동 백업, CloudFront CDN으로 빠르고 안전한 서비스를 제공합니다.
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
                                의료 업무의 새로운 기준
                            </p>
                        </div>
                        <div className={styles.footerLinks}>
                            <a href="/terms" className={styles.footerLink}>이용약관</a>
                            <a href="/privacy" className={styles.footerLink}>개인정보처리방침</a>
                        </div>
                    </div>
                    <div className={styles.footerBottom}>
                        <p>&copy; 2025 MediFlow. All rights reserved.</p>
                    </div>
                </div>
            </footer>
        </div>
    );
};

export default LandingPage;
