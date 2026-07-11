export interface SseEvent {
  event: string;
  data: string;
}

export function parseSseBlock(block: string): SseEvent | null {
  const lines = block.split(/\r?\n/);
  const eventLine = lines.find((line) => line.startsWith('event:'));
  const dataLines = lines.filter((line) => line.startsWith('data:'));
  if (dataLines.length === 0) {
    return null;
  }
  return {
    event: eventLine ? eventLine.replace('event:', '').trim() : 'message',
    data: dataLines.map((line) => line.replace('data:', '').trim()).join('\n')
  };
}

export function parseSseText(text: string): SseEvent[] {
  return text
    .split(/\r?\n\r?\n/)
    .map((block) => parseSseBlock(block.trim()))
    .filter((event): event is SseEvent => event !== null);
}

export class SseStreamParser {
  private buffer = '';

  push(chunk: string): SseEvent[] {
    this.buffer += chunk;
    const blocks = this.buffer.split(/\r?\n\r?\n/);
    this.buffer = blocks.pop() || '';
    return blocks
      .map((block) => parseSseBlock(block.trim()))
      .filter((event): event is SseEvent => event !== null);
  }

  flush(): SseEvent[] {
    const pending = this.buffer.trim();
    this.buffer = '';
    const event = pending ? parseSseBlock(pending) : null;
    return event ? [event] : [];
  }
}
