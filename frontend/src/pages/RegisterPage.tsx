import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerUser } from '../utils/api';
import Toast from '../components/Toast';
import { Button } from '@/components/ui/button';
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { MagicCard } from '@/components/ui/magic-card';
import DotGrid from '@/components/ui/DotGrid';

const RegisterPage: React.FC = () => {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            setLoading(true);
            await registerUser({ username, password, email });
            setToast({ message: 'User registered successfully!', type: 'success' });
            setTimeout(() => navigate('/login'), 1500);
        } catch (err: any) {
            const errorMsg =
                err.response?.data || 'Registration failed. Please try again.';
            setToast({ message: String(errorMsg), type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="dark min-h-screen bg-background flex flex-col items-center justify-center p-6 relative overflow-hidden">
            {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

            {/* Background Dot Grid */}
            <div className="absolute inset-0 z-0">
                <DotGrid
                    dotSize={5}
                    gap={15}
                    baseColor="#271E37"
                    activeColor="#5227FF"
                    proximity={120}
                    shockRadius={250}
                    shockStrength={5}
                    resistance={750}
                    returnDuration={1.5}
                />
            </div>

            <Card className="relative z-10 w-full max-w-sm border-none p-0 shadow-none">
                <MagicCard
                    gradientColor="#262626"
                    className="p-0"
                >
                    <CardHeader className="border-border border-b p-4 [.border-b]:pb-4">
                        <CardTitle>Create Account</CardTitle>
                        <CardDescription>
                            Join Huddle and start collaborating
                        </CardDescription>
                    </CardHeader>
                    <CardContent className="p-4">
                        <form id="register-form" onSubmit={handleSubmit}>
                            <div className="grid gap-4">
                                <div className="grid gap-2">
                                    <Label htmlFor="register-username">Username</Label>
                                    <Input
                                        id="register-username"
                                        type="text"
                                        placeholder="johndoe"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="grid gap-2">
                                    <Label htmlFor="register-email">Email</Label>
                                    <Input
                                        id="register-email"
                                        type="email"
                                        placeholder="name@example.com"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="grid gap-2">
                                    <Label htmlFor="register-password">Password</Label>
                                    <Input
                                        id="register-password"
                                        type="password"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        required
                                    />
                                </div>
                            </div>
                        </form>
                    </CardContent>
                    <CardFooter className="border-border border-t p-4 [.border-t]:pt-4 flex flex-col gap-3">
                        <Button
                            id="register-submit"
                            type="submit"
                            form="register-form"
                            className="w-full"
                            disabled={loading}
                        >
                            {loading ? 'Signing Up...' : 'Sign Up'}
                        </Button>
                        <p className="text-sm text-muted-foreground text-center">
                            Already have an account?{' '}
                            <Link to="/login" className="text-primary underline underline-offset-2 hover:opacity-80">
                                Log In
                            </Link>
                        </p>
                    </CardFooter>
                </MagicCard>
            </Card>
        </div>
    );
};

export default RegisterPage;
