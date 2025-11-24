import { useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import styles from './TimeSeriesChart.module.scss';

/**
 * 시계열 데이터 차트 컴포넌트 (재사용 가능)
 * 바이탈, I/O 등 시간에 따른 데이터 변화를 시각화
 */
const TimeSeriesChart = ({ data, metrics, title }) => {
  const [selectedMetrics, setSelectedMetrics] = useState(
    metrics.map(m => m.key)
  );

  // 메트릭 선택/해제
  const toggleMetric = (key) => {
    setSelectedMetrics(prev => 
      prev.includes(key) 
        ? prev.filter(k => k !== key)
        : [...prev, key]
    );
  };

  // 시간 포맷 (간결하게)
  const formatTime = (dateTime) => {
    const date = new Date(dateTime);
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${month}/${day} ${hours}:${minutes}`;
  };

  // 데이터가 없으면 빈 상태 표시
  if (!data || data.length === 0) {
    return (
      <div className={styles.emptyChart}>
        <p>표시할 데이터가 없습니다</p>
      </div>
    );
  }

  return (
    <div className={styles.chartContainer}>
      {title && <h4 className={styles.chartTitle}>{title}</h4>}
      
      {/* 메트릭 필터 */}
      <div className={styles.metricFilters}>
        {metrics.map(metric => (
          <button
            key={metric.key}
            className={`${styles.filterButton} ${
              selectedMetrics.includes(metric.key) ? styles.active : ''
            }`}
            onClick={() => toggleMetric(metric.key)}
            style={{
              borderColor: selectedMetrics.includes(metric.key) ? metric.color : '#d1d5db'
            }}
          >
            <span 
              className={styles.colorDot} 
              style={{ backgroundColor: metric.color }}
            />
            {metric.label}
          </button>
        ))}
      </div>

      {/* 차트 */}
      <ResponsiveContainer width="100%" height={350}>
        <LineChart 
          data={data} 
          margin={{ top: 10, right: 10, left: 0, bottom: 20 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis 
            dataKey="time" 
            tickFormatter={formatTime}
            angle={-30}
            textAnchor="end"
            height={60}
            tick={{ fontSize: 11, fill: '#6b7280' }}
            stroke="#d1d5db"
          />
          <YAxis 
            tick={{ fontSize: 11, fill: '#6b7280' }}
            stroke="#d1d5db"
          />
          <Tooltip 
            labelFormatter={formatTime}
            contentStyle={{ 
              backgroundColor: 'white', 
              border: '1px solid #e5e7eb',
              borderRadius: '0.375rem',
              fontSize: '0.875rem',
              padding: '0.5rem'
            }}
            labelStyle={{ 
              fontWeight: 600, 
              marginBottom: '0.25rem',
              fontSize: '0.75rem'
            }}
          />
          <Legend 
            wrapperStyle={{ fontSize: '0.75rem' }}
            iconSize={10}
          />
          
          {metrics.map(metric => 
            selectedMetrics.includes(metric.key) && (
              <Line
                key={metric.key}
                type="monotone"
                dataKey={metric.key}
                name={metric.label}
                stroke={metric.color}
                strokeWidth={2}
                dot={{ r: 3, fill: metric.color }}
                activeDot={{ r: 5 }}
              />
            )
          )}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default TimeSeriesChart;
