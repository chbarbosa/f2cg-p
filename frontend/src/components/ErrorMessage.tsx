interface Props {
  message: string;
}

export function ErrorMessage({ message }: Props) {
  return (
    <div className="error-box">
      <span>⚠</span>
      <p>{message}</p>
    </div>
  );
}