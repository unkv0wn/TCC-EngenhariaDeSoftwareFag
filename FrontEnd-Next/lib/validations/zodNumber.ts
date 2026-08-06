import { z } from "zod";

// zod v4 rejects NaN at the base number type check — and react-hook-form's valueAsNumber
// turns an empty numeric input into NaN, not undefined. A custom error function on the
// invalid_type issue keeps the message in Portuguese instead of falling through to zod's
// default "Invalid input: expected number, received NaN".
export function requiredNumber(message: string) {
  return z.number({
    error: (issue) => (issue.code === "invalid_type" ? message : undefined),
  });
}
