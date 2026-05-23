import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getExperiences, getProjects, getEducation,deleteExperience } from "../api/resume";
import type { ResumeExperience, ResumeProject, ResumeEducation, Degree } from "../types";
import { ExperienceModal } from "../components/resume/ExperienceModal";
import { Button } from "../components/ui/Button";
import { useToast } from "../components/ui/Toast";

const DEGREE_LABELS: Record<Degree, string> = {
    BACHELOR: 'Бакалавриат', 'MASTER': 'Магистратура', SPECIALIST: 'Специалитет', PHD: 'Аспирантура', COLLEGE: 'Колледж', OTHER: 'Другое',
};



function fmtMonth(value: string | null): string {
    if (!value) return 'настоящее время';
    const [y, m] = value.split('-');
    const months = ['', 'янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
    return `${months[Number(m)] || m} ${y}`;
}


export function ResumePage() {
    const navigate = useNavigate();
    const [experiences, setExperiences] = useState<ResumeExperience[]>([]);
    const [projects, setProjects] = useState<ResumeProject[]>([]);
    const [education, setEducation] = useState<ResumeEducation[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);


    const { showToast } = useToast();
    const [expModal, setExpModal] = useState<{ open: boolean; record: ResumeExperience | null }>({ open: false, record: null });

    async function handleDeleteExperience(id: string) {
        if (!window.confirm('Удалить эту запись об опыте?')) return;
        try {
            await deleteExperience(id);
            showToast('Запись удалена', 'success');
            load();
        } catch (e: any) {
            showToast(e?.response?.data?.error?.message || 'Не удалось удалить', 'error');
        }
    }

    const iconBtn = { background: 'none', border: 'none', cursor: 'pointer', padding: '0.25rem', color: 'var(--color-text-secondary)' } as const;

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const [exp, proj, edu] = await Promise.all([getExperiences(), getProjects(), getEducation()]);
            setExperiences(exp);
            setProjects(proj);
            setEducation(edu);
        } catch {
            setError('Не удалось загрузить резюме. Обновите страницу.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    const wrap = { maxWidth: 800, margin: '2rem auto', padding: '0 1rem' } as const;
    const card = { background: 'var(--color-bg-subtle, #f7f7f7)', border: '1px solid var(--color-border)', borderRadius: 12, padding: '1.25rem', marginBottom: '1rem' } as const;
    const title = { fontSize: '1.1rem', fontWeight: 600, marginBottom: '0.75rem' } as const;
    const empty = { color: 'var(--color-text-secondary)', fontSize: '0.9rem' } as const;
    const item = { padding: '0.75rem 0', borderBottom: '1px solid var(--color-border)' } as const;
    const meta = { color: 'var(--color-text-secondary)', fontSize: '0.85rem' } as const;

    if (loading) return <div style={wrap}>Загрузка резюме…</div>;
    if (error) return (
        <div style={wrap}>
            <p style={{ color: '#dc2626' }}>{error}</p>
            <button onClick={load}>Повторить</button>
        </div>
    );

    return (
        <div style={wrap}>
            <button onClick={() => navigate('/profile')}
                style={{ background: 'none', border: 'none', color: 'var(--color-accent)', cursor: 'pointer', marginBottom: '1rem' }}>
                <span className="material-symbols-rounded">arrow_back</span> Назад в кабинет
            </button>
            <h1 style={{ marginBottom: '1.5rem' }}>Моё резюме</h1>


            {/* Опыт работы */}
            <section style={card}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                    <div style={title}>Опыт работы</div>
                    <Button variant="secondary" size="sm" onClick={() => setExpModal({ open: true, record: null })}>
                        <span className="material-symbols-rounded">add</span>Добавить
                    </Button>
                </div>
                {experiences.length === 0
                    ? <div style={empty}>Пока нет записей об опыте работы.</div>
                    : experiences.map(e => (
                        <div key={e.id} style={item}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem' }}>
                                <div>
                                    <div style={{ fontWeight: 600 }}>{e.position}</div>
                                    <div style={meta}>{e.organization} · {fmtMonth(e.startDate)} — {fmtMonth(e.endDate)}</div>
                                    {e.description && <div style={{ marginTop: 4 }}>{e.description}</div>}
                                </div>
                                <div style={{ display: 'flex', gap: '0.25rem', flexShrink: 0 }}>
                                    <button title="Редактировать" style={iconBtn} onClick={() => setExpModal({ open: true, record: e })}>
                                        <span className="material-symbols-rounded" style={{ fontSize: 18 }}>edit</span>
                                    </button>
                                    <button title="Удалить" style={iconBtn} onClick={() => handleDeleteExperience(e.id)}>
                                        <span className="material-symbols-rounded" style={{ fontSize: 18 }}>delete</span>
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
            </section>

            {/* Проекты */}
            <section style={card}>
                <div style={title}>Проекты</div>
                {projects.length === 0
                    ? <div style={empty}>Пока нет проектов.</div>
                    : projects.map(p => (
                        <div key={p.id} style={item}>
                            <div style={{ fontWeight: 600 }}>{p.title}{p.role ? ` · ${p.role}` : ''}</div>
                            <div style={meta}>{fmtMonth(p.startDate)} — {fmtMonth(p.endDate)}</div>
                            {p.description && <div style={{ marginTop: 4 }}>{p.description}</div>}
                            <div style={{ marginTop: 4, display: 'flex', gap: '1rem' }}>
                                {p.projectUrl && <a href={p.projectUrl} target="_blank" rel="noreferrer">Демо</a>}
                                {p.repositoryUrl && <a href={p.repositoryUrl} target="_blank" rel="noreferrer">Репозиторий</a>}
                            </div>
                        </div>
                    ))}
            </section>

            {/* Образование */}
            <section style={card}>
                <div style={title}>Образование</div>
                {education.length === 0
                    ? <div style={empty}>Пока нет записей об образовании.</div>
                    : education.map(ed => (
                        <div key={ed.id} style={item}>
                            <div style={{ fontWeight: 600 }}>{ed.institution}</div>
                            <div style={meta}>
                                {DEGREE_LABELS[ed.degree]}{ed.faculty ? ` · ${ed.faculty}` : ''} · {ed.startYear} — {ed.endYear ?? 'наст. время'}
                            </div>
                            {ed.description && <div style={{ marginTop: 4 }}>{ed.description}</div>}
                            </div>
                    ))}
            </section>

            <ExperienceModal
                isOpen={expModal.open}
                record={expModal.record}
                onClose={() => setExpModal({ open: false, record: null })}
                onSaved={load}
            />
        </div>
    );
}