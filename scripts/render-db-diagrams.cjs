/** Code-native schema overview. Run with CODEX_NODE_MODULES pointing to bundled node_modules. */
const fs = require('fs');
const path = require('path');
const sharp = require(path.join(process.env.CODEX_NODE_MODULES, 'sharp'));
const groups = [
 ['Core / Assignment', '#0369a1', [
  ['department', 'PK department_id', 'name · code · bed_count · type'],
  ['users', 'PK user_id', 'FK department_id → department', 'email · role · provider · account status'],
  ['patient', 'PK patient_id', 'FK department_id → department', 'chart_number · diagnosis · room · risk flags'],
  ['shift', 'PK shift_id', 'date · type · start_time · end_time'],
  ['assignment', 'PK assignment_id', 'FK nurse_id → users · patient_id → patient', 'FK shift_id → shift · assigned_date'],
  ['onboarding', 'PK onboarding_id', 'FK nurse_id → users', 'step · is_completed · completed_at'],
 ]],
 ['Clinical Records', '#047857', [
  ['vital_sign', 'PK vital_sign_id', 'FK patient_id → patient · nurse_id → users', 'BP · HR · BT · SpO2 · measured_at'],
  ['nursing_note', 'PK nursing_note_id', 'FK patient_id → patient · nurse_id → users', 'content · plain_text · category · is_important'],
  ['medical_orders', 'PK id', 'FK patient_id → patient', 'order details · status · ordered_at'],
  ['medication', 'PK medication_id', 'FK patient_id → patient · nurse_id → users', 'FK medical_order_id → medical_orders'],
  ['intake_output', 'PK intake_output_id', 'FK patient_id → patient · nurse_id → users', 'intake · output · recorded_at'],
  ['test_results', 'PK test_result_id', 'FK patient_id → patient · nurse_id → users', 'test details · reference · result'],
  ['medical_image', 'PK medical_image_id', 'FK patient_id → patient', 'image_type · image_url · exam_date'],
 ]],
 ['Handover / Supporting', '#7c3aed', [
  ['handover', 'PK handover_id', 'FK department_id · from_shift_id · to_shift_id', 'FK created_by → users · version', 'source_type · status DRAFT/APPROVED', 'ai_summary · final_summary · ai_model · approved_at'],
  ['handover_revision', 'PK revision_id · index (handover_id, revision_id)', 'handover_id / edited_by_id: snapshot IDs (no FK)', 'record_version · status · source_type · recorded_at', 'ai_summary · final_summary · edited_by_name'],
  ['clinical_audit_log', 'PK audit_log_id', 'user_id / patient_id: audit IDs (no FK)', 'action · resource_type · resource_id · occurred_at'],
  ['chat_history', 'PK chat_history_id', 'FK nurse_id → users', 'message · response · created_at'],
  ['email_verification', 'PK verification_id', 'FK user_id → users', 'email · verification_code · expiry_date', 'is_verified · failed_attempts (max 5)'],
 ]],
];
const esc = x => x.replaceAll('&','&amp;').replaceAll('<','&lt;');
const width=1860,height=1630,step=188;
let svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}"><style>text{font-family:Arial,sans-serif;fill:#1f2937}.title{font-size:36px;font-weight:700}.head{font-size:25px;font-weight:700;fill:white}.name{font-size:25px;font-weight:700}.line{font-size:19px}.small{font-size:20px;fill:#4b5563}</style><rect width="100%" height="100%" fill="#f8fafc"/><text x="36" y="54" class="title">MediFlow database · 18 entity tables · 2026-10-11</text><text x="36" y="88" class="small">MariaDB · FK relations listed inside cards · snapshot IDs deliberately have no physical FK</text>`;
groups.forEach(([label,color,tables],index)=>{let x=30+index*610;svg+=`<rect x="${x}" y="120" width="590" height="1400" rx="14" fill="#fff" stroke="#cbd5e1"/><rect x="${x}" y="120" width="590" height="52" rx="10" fill="${color}"/><text x="${x+18}" y="155" class="head">${label}</text>`;tables.forEach((lines,i)=>{let y=190+i*step;svg+=`<rect x="${x+14}" y="${y}" width="562" height="174" rx="8" fill="#f8fafc" stroke="#dbe4ee"/><text x="${x+30}" y="${y+32}" class="name">${esc(lines[0])}</text>`;lines.slice(1).forEach((line,j)=>svg+=`<text x="${x+30}" y="${y+60+j*25}" class="line">${esc(line)}</text>`);});});
svg+=`<text x="36" y="1560" class="small">BaseTimeEntity tables also contain version (@Version), created_at and updated_at.</text><text x="36" y="1593" class="small">Flyway adds flyway_schema_history in migrated environments; it is infrastructure metadata, not an entity table.</text></svg>`;
(async()=>{for(const base of ['assets','portfolio-docs/assets']){fs.writeFileSync(base+'/db.svg',svg);await sharp(Buffer.from(svg)).png().toFile(base+'/db.png');}for(const name of ['erd','table'])fs.copyFileSync('portfolio-docs/assets/db.png',`portfolio-docs/assets/${name}.png`);console.log('Rendered DB diagrams from source.');})();
